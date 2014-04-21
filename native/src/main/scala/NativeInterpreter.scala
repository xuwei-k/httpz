package httpz
package native

import scalaz.{One => _, Two => _, _}

object NativeInterpreter extends InterpretersTemplate {

  protected override def request2string(req: Request) =
    HttpzNative(req).asString

  protected[this] override def onHttpError[A](o: RequestF.One[A], e: Throwable): A =
    e match {
      case HttpException(code, msg, body, cause) =>
        val str = Iterator(
          "code" -> code, "message" -> msg, "body" -> body, "cause" -> cause
        ).mkString("HttpError(", ",", ")")
        o.error(Error.httpWithToString(e, str))
      case _ =>
        super.onHttpError(o, e)
    }

}

