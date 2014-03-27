package httpz

import scalaz.\/
import RequestF._

sealed abstract class RequestF[A] extends Product with Serializable {
  def mapRequest(config: Config): RequestF[A] = this match {
    case o @ One() => o.cp(req = config(o.req))
    case t @ Two() => two(t.x.mapRequest(config), t.y.mapRequest(config))(t.f)
  }
}

object RequestF {

  sealed abstract case class One[A]() extends RequestF[A] {
    type B
    def req: Request
    def error: Error => A
    def parse: (Request, String) => B
    def decode: (Request, B) => A
    override final def toString = s"RequestF.One(${req.method} ${req.url})"
    def cp(
      req: Request = req, error: Error => A = error, parse: (Request, String) => B = parse, decode: (Request, B) => A = decode
    ): RequestF[A] = one(req, error, parse, decode)
  }

  sealed abstract case class Two[A]() extends RequestF[A] {
    type X
    type Y
    type E1
    type E2
    def x: ActionE[E1, X]
    def y: ActionE[E2, Y]
    def f: (E1 \/ X, E2 \/ Y) => A
  }

  def one[A, B0](req0: Request, error0: Error => A, parse0: (Request, String) => B0, decode0: (Request, B0) => A): RequestF[A] =
    new One[A]{
      type B = B0
      def req = req0
      def error = error0
      def parse = parse0
      def decode = decode0
    }

  def two[X0, Y0, EE1, EE2, A](x0: ActionE[EE1, X0], y0: ActionE[EE2, Y0])(f0: (EE1 \/ X0, EE2 \/ Y0) => A): RequestF[A] =
    new Two[A] {
      type X = X0
      type Y = Y0
      type E1 = EE1
      type E2 = EE2
      def x = x0
      def y = y0
      def f = f0
    }

}

