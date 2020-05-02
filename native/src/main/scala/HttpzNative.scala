package httpz
package native

object HttpzNative {

  def apply(req: httpz.Request): Http.Request = {
    val r0 = req.method match {
      case "GET" =>
        Http.get(req.url)
      case "POST" =>
        req.body match {
          case None => Http.post(req.url)
          case Some(bytes) => Http.postData(req.url, bytes)
        }
      case other =>
        Http(req.url).method(other)
    }
    val r1 = r0.params(req.params).headers(req.headers)
    req.basicAuth.fold(r1) { case (user, pass) => r1.auth(user, pass) }
  }

}
