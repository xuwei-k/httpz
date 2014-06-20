package httpz

import scalaz._, Free.FreeC
import argonaut._

object Core extends Core[RequestF] {
  /**
   * @see [[https://dl.dropboxusercontent.com/u/4588997/ReasonablyPriced.pdf]]
   * @see [[https://gist.github.com/runarorama/a8fab38e473fafa0921d]]
   */
  implicit def instance[F[_]](implicit I: Inject[RequestF, F]) =
    new Core[F]
}

sealed class Core[F[_]](implicit I: Inject[RequestF, F]) {

  private[this] def lift[A, B](f: RequestF[A \/ B]) =
    EitherT[({type l[a] = FreeC[F, a]})#l, A, B](Free.liftFC(I.inj(f)))

  def json[A](req: Request)(implicit A: DecodeJson[A]): EitherT[({type l[a] = FreeC[F, a]})#l, Error, A] =
    lift(RequestF.one[Error \/ A, Error \/ Json](
      req,
      \/.left,
      (request, result) => Parse.parse(result).leftMap(Error.parse),
      (request, either) => either.flatMap{ json =>
        A.decodeJson(json).result match {
           case r @ \/-(_) => r
           case -\/((msg, history)) => -\/(Error.decode(request, msg, history, json))
        }
      }
    ))

  def string(req: Request): EitherT[({type l[a] = FreeC[F, a]})#l, Throwable, String] =
    lift(RequestF.one[Throwable \/ String, String](
      req,
      \/.left,
      (_, result) => result,
      (_, result) => \/-(result)
    ))

}

