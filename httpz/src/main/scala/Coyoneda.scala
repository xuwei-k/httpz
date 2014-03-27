package scalaz

// workaround. will be remove this file
// argonaut 6.1.x still use scalaz 7.1.0-M3
// https://github.com/argonaut-io/argonaut/blob/v6.1-M2/project/build.scala#L14
abstract class Coyoneda[F[_], A] { coyo =>
  type I
  def fi: F[I]
  def k(i: I): A

  /** Converts to `F[A]` given that `F` is a functor */
  def run(implicit F: Functor[F]): F[A] =
    F.map(fi)(k)

  /** Simple function composition. Allows map fusion without touching the underlying `F`. */
  def map[B](f: A => B): Coyoneda[F, B] = new Coyoneda[F, B] {
    type I = coyo.I
    val fi = coyo.fi
    def k(i: I) = f(coyo k i)
  }

  def trans[G[_]](f: F ~> G): Coyoneda[G, A] = new Coyoneda[G, A] {
    type I = coyo.I
    val fi = f(coyo.fi)
    def k(i: I) = coyo k i
  }
}

object Coyoneda {

  /** `F[A]` converts to `Coyoneda[F,A]` for any `F` */
  def apply[F[_], A](fa: F[A]): Coyoneda[F, A] = new Coyoneda[F, A] {
    type I = A
    def k(a: A) = a
    val fi = fa
  }

  /** `Coyoneda[F,_]` is a functor for any `F` */
  implicit def coyonedaFunctor[F[_]]: Functor[({type λ[α] = Coyoneda[F,α]})#λ] =
    new Functor[({type λ[α] = Coyoneda[F,α]})#λ] {
      def map[A,B](ya: Coyoneda[F,A])(f: A => B) = ya map f
    }

}

