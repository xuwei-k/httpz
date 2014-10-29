package httpz

import scalaz._, Free._

object Z {

  private[httpz] def freeCMonad[S[_]]: Monad[({type λ[α] = FreeC[S, α]})#λ] =
    Free.freeMonad[({type λ[α] = Coyoneda[S, α]})#λ]

  def mapSuspensionFreeC[F[_], G[_], A](c: FreeC[F, A], f: F ~> G): FreeC[G, A] = {
    type CoyonedaG[B] = Coyoneda[G, B]
    c.mapSuspension[CoyonedaG](new (({type λ[α] = Coyoneda[F, α]})#λ ~> CoyonedaG){
      def apply[C](a: Coyoneda[F, C]) = a.trans(f)
    })
  }
}

