package httpz
package scalajhttp

import scalaj.http._

object ScalajHttp {

  val OPTIONS = List(HttpOptions.connTimeout(30000), HttpOptions.readTimeout(30000))

  def apply(req: httpz.Request): HttpRequest = {
    val r0 = req.method match {
      case "GET" =>
        Http(req.url)
      case "POST" =>
        req.body match {
          case None => Http(req.url).method("POST")
          case Some(bytes) => Http(req.url).postData(bytes)
        }
      case other =>
        Http(req.url).method(other)
    }
    val r1 = r0.params(req.params).headers(req.headers).options(OPTIONS)
    req.basicAuth.fold(r1) { case (user, pass) => r1.auth(user, pass) }
  }

}
