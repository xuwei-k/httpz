package httpz

import scalaz.{One => _, Two => _, _}

final class ActionEOps[E, A](val self: ActionE[E, A]) extends AnyVal {

  def nel: ActionE[NonEmptyList[E], A] = self.leftMap(NonEmptyList.nel(_, Nil))

  def mapRequest(f: Config): ActionE[E, A] = Action[E, A](
    self.run.mapSuspension(new (RequestF ~> RequestF){
      def apply[B](a: RequestF[B]) = a.mapRequest(f)
    })
  )

  def interpretBy[F[_]: Monad](f: InterpreterF[F]): F[E \/ A] =
    self.run foldMap f

  def zipWithError[B, C, E1, E2](that: ActionE[E1, B])(f: (E \/ A, E1 \/ B) => E2 \/ C): ActionE[E2, C] =
    Action(liftF(RequestF.two(self, that)(f)))

  def zip[B](that: ActionE[E, B])(implicit E: Semigroup[E]): ActionE[E, (A, B)] =
    zipWith(that)(Tuple2.apply)

  import syntax.apply._

  def zipWith[B, C](that: ActionE[E, B])(f: (A, B) => C)(implicit E: Semigroup[E]): ActionE[E, C] =
    zipWithError(that)((a, b) => (a.validation |@| b.validation)(f).disjunction)

}
