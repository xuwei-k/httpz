package httpz

sealed abstract class Time{
  type A
  def request: Request
  def http: Long
  def result: A
}

object Time {
  final case class Success(
    request: Request, result: String, http: Long, parse: Long, decode: Long
  ) extends Time {
    type A = String
    override def toString = {
      ("request" -> request.toString) +: Seq(
        "http" -> http,
        "parse" -> parse,
        "decode" -> decode
      ).map{case (k, v) => k -> (v / 1000000.0).toString}
    }.map{case (k, v) => s"${k.toString} = ${v.toString}"}.mkString("Time.Success(", ", ", ")")
  }

  final case class Failure(
    request: Request, result: Throwable, http: Long
  ) extends Time {
    type A = Throwable
    override def toString = s"Time.Failure(request = ${request}, http = ${http / 1000000.0})"
  }
}

