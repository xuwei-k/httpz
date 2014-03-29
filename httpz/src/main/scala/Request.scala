package httpz

import scalaz.Endo

final case class Request(
  url: String,
  method: String = "GET",
  body: Option[Array[Byte]] = None,
  params: Map[String, String] = Map.empty,
  headers: Map[String, String] = Map.empty,
  basicAuth: Option[(String, String)] = None
) {

  def addParam(k: String, v: String): Request =
    copy(params = this.params + (k -> v))

  def addParams(p: (String, String) *): Request =
    copy(params = this.params ++ p.toMap)

  def addHeader(k: String, v: String): Request =
    copy(headers = this.headers + (k -> v))

  def addHeaders(p: (String, String) *): Request =
    copy(headers = this.headers ++ p.toMap)

  /** Basic authentication */
  def auth(user: String, pass: String): Request =
    copy(basicAuth = Some(user -> pass))

  /** @see [[http://tools.ietf.org/html/rfc6750]] */
  def bearer(token: String): Request =
    copy(headers = this.headers + ("Authorization" -> ("Bearer " + token)))
}

object Request {

  def bearer(token: String): Config =
    Endo(_.bearer(token))

  def header(k: String, v: String): Config =
    Endo(_.addHeader(k, v))

  def headers(p: (String, String) *): Config =
    Endo(_.addHeaders(p: _*))

  def param(k: String, v: String): Config =
    Endo(_.addParam(k, v))

  def params(p: (String, String) *): Config =
    Endo(_.addParams(p: _*))

  def auth(user: String, pass: String): Config =
    Endo(_.auth(user, pass))
}

