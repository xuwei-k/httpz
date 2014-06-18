package httpz

import scalaz._, Free._

object Z {

  // TODO will be remove https://github.com/scalaz/scalaz/pull/731/files
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

