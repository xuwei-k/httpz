package httpz

import scalaz._, Free._

// workaround. will be remove some methods
// argonaut 6.1.x still use scalaz 7.1.0-M3
// https://github.com/argonaut-io/argonaut/blob/v6.1-M2/project/build.scala#L14
object Z {

  // https://github.com/scalaz/scalaz/blob/v7.1.0-M5/core/src/main/scala/scalaz/Free.scala#L35
  type FreeC[S[_], A] = Free[({type f[x] = Coyoneda[S, x]})#f, A]

  def liftF[S[_], A](value: => S[A])(implicit S: Functor[S]): Free[S, A] =
    Suspend(S.map(value)(Return[S, A]))

  def liftFU[MA](value: => MA)(implicit MA: Unapply[Functor, MA]): Free[MA.M, MA.A] =
    liftF(MA(value))(MA.TC)

  def freeC[S[_], A](value: S[A]): FreeC[S, A] =
    liftFU(Coyoneda(value))

  final def interpret[M[_], N[_], A](free: FreeC[N, A])(f: N ~> M)(implicit M: Monad[M]): M[A] = {
    def go(a: FreeC[N, A]): M[A] = a.resume match {
      case \/-(c) => M.point(c)
      case -\/(c) => M.bind(f(c.fi))(x => go(c.k(x)))
    }
    go(free)
  }

  private[httpz] def freeCMonad[S[_]]: Monad[({type λ[α] = FreeC[S, α]})#λ] =
    Free.freeMonad[({type λ[α] = Coyoneda[S, α]})#λ]

  def mapSuspensionFreeC[F[_], G[_], A](c: FreeC[F, A], f: F ~> G): FreeC[G, A] = {
    type CoyonedaG[A] = Coyoneda[G, A]
    c.mapSuspension[CoyonedaG](new (({type λ[α] = Coyoneda[F, α]})#λ ~> CoyonedaG){
      def apply[A](a: Coyoneda[F, A]) = a.trans(f)
    })
  }
}

