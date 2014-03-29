package httpz
package scalajhttp

import scalaj.http._
import scalaz.Endo

object ScalajHttp{

  val OPTIONS = List(HttpOptions.connTimeout(30000), HttpOptions.readTimeout(30000))

  def apply(req: httpz.Request): Http.Request = {
    val r0 = req.method match {
      case "GET"      => Http.get(req.url)
      case "POST"     => req.body match {
        case None        => Http.post(req.url)
        case Some(bytes) => Http.postData(req.url, bytes)
      }
    }
    val r1 = r0.params(req.params).headers(req.headers).options(OPTIONS)
    req.basicAuth.fold(r1){case (user, pass) => r1.auth(user, pass)}
  }

}

