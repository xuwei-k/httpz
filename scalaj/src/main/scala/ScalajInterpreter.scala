package httpz
package scalajhttp

import scalaz.{One => _, Two => _, _}

object ScalajInterpreter extends InterpretersTemplate {

  protected override def request2string(req: Request) =
    ScalajHttp(req).asString

  protected[this] override def onHttpError[A](o: RequestF.One[A], e: Throwable): A =
    e match {
      case scalaj.http.HttpException(code, msg, body, cause) =>
        val str = Iterator(
          "code" -> code, "message" -> msg, "body" -> body, "cause" -> cause
        ).mkString("HttpError(", ",", ")")
        o.error(Error.httpWithToString(e, str))
      case _ =>
        super.onHttpError(o, e)
    }

}

