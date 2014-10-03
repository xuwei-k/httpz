package httpz

import scalaz.{EitherT, Free, Monad, \/, ~>}

abstract class NaturalTransMonad[F[_], G[_]: Monad] { self =>
  final val interpreter: F ~> G =
    new (F ~> G) {
      def apply[A](a: F[A]) = go(a)
    }

  protected[this] def go[A](a: F[A]): G[A]

  final def run[E, A](a: EitherT[({type l[a] = Free.FreeC[F, a]})#l, E, A]): G[E \/ A] =
    Free.runFC(a.run)(interpreter)

  final def contramap[F0[_]](f: F0 ~> F): NaturalTransMonad[F0, G] =
    new NaturalTransMonad[F0, G] {
      override protected[this] def go[A](a: F0[A]): G[A] =
        self.go(f(a))
    }
}


