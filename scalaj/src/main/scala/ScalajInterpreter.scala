package httpz
package scalajhttp

object ScalajInterpreter extends InterpretersTemplate {

  protected override def request2string(req: Request) =
    ScalajHttp(req).asString.body

  protected[this] override def onHttpError[A](o: RequestF.One[A], e: Throwable): A =
    e match {
      case scalaj.http.HttpException(msg, cause) =>
        val str = Iterator(
          "message" -> msg, "cause" -> cause
        ).mkString("HttpError(", ",", ")")
        o.error(Error.httpWithToString(e, str))
      case _ =>
        super.onHttpError(o, e)
    }

}

