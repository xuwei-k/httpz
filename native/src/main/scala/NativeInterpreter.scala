package httpz
package native

object NativeInterpreter extends InterpretersTemplate {

  protected override def request2response(req: Request) = {
    val r = HttpzNative(req)

    r.process { c =>
      val status = c.getResponseCode
      val headers = r.getResponseHeaders(c)
      val in = c.getInputStream
      val body =
        try {
          new ByteArray(Core.inputStream2bytes(in))
        } finally {
          in.close()
        }
      Response(body, status, headers)
    }
  }

  protected[this] override def onHttpError[A](o: RequestF.One[A], e: Throwable): A =
    e match {
      case HttpException(code, msg, body, cause) =>
        val str = Iterator(
          "code" -> code,
          "message" -> msg,
          "body" -> body,
          "cause" -> cause
        ).mkString("HttpError(", ",", ")")
        o.error(Error.httpWithToString(e, str))
      case _ =>
        super.onHttpError(o, e)
    }

}
