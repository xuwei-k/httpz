import scalaz._

import scala.concurrent.Future

package object httpz {

  type InterpreterF[F[_]] = RequestF ~> F

  type Requests[A] = Free[RequestF, A]

  type ErrorNel = NonEmptyList[Error]

  type ActionE[E, A] = EitherT[E, Requests, A]
  type Action[A] = EitherT[Error, Requests, A]
  type ActionNel[A] = EitherT[ErrorNel, Requests, A]

  def Action[E, A](a: Requests[E \/ A]): ActionE[E, A] = EitherT(a)

  implicit def toActionEOps[E, A](a: ActionE[E, A]): ActionEOps[E, A] = new ActionEOps(a)

  type Config = Endo[Request]

  type Times[A] = Writer[List[Time], A]
  private[httpz] def Times[A](a: (List[Time], A)): Times[A] =
    Writer(a._1, a._2)

  private[httpz] type FutureTimes[A] = WriterT[List[Time], Future, A]

  private[httpz] implicit val timesMonad: Monad[Times] =
    scalaz.WriterT.writerMonad[List[Time]](scalaz.std.list.listMonoid)

  val emptyConfig: Config = Endo.idEndo

  val RequestsMonad: Monad[Requests] =
    Free.freeMonad[RequestF]

  def actionEMonad[E]: Monad[({ type λ[α] = ActionE[E, α] })#λ] =
    EitherT.eitherTMonadError[Requests, E]

  val ActionMonad: Monad[Action] =
    actionEMonad[Error]

  val ActionNelMonad: Monad[ActionNel] =
    actionEMonad[ErrorNel]

  def ActionZipAp[E: Semigroup]: Apply[({ type λ[α] = ActionE[E, α] })#λ] =
    new Apply[({ type λ[α] = ActionE[E, α] })#λ] {
      override def ap[A, B](fa: => ActionE[E, A])(f: => ActionE[E, A => B]) =
        f.zipWith(fa)(_ apply _)

      override def map[A, B](fa: ActionE[E, A])(f: A => B) =
        fa map f

      override def apply2[A, B, C](fa: => ActionE[E, A], fb: => ActionE[E, B])(f: (A, B) => C) =
        fa.zipWith(fb)(f)
    }

  val ActionNelZipAp: Apply[ActionNel] =
    ActionZipAp[ErrorNel]
}
