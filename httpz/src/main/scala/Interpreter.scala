package httpz

import scalaz.{One => _, Two => _, _}

abstract class Interpreter[F[_]: Monad] {
  final val interpreter: InterpreterF[F] =
    new InterpreterF[F] {
      def apply[A](a: RequestF[A]) = go(a)
    }

  protected[this] def go[A](a: RequestF[A]): F[A]

  final def run[E, A](a: ActionE[E, A]): F[E \/ A] =
    Z.interpret(a.run)(interpreter)
}

