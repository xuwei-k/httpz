package httpz

import argonaut._

sealed abstract class Error(value: Option[Throwable] = None) extends RuntimeException(value.orNull) with Product with Serializable {
  import Error._

  def httpOr[A](z: => A, f: Throwable => A): A =
    this match {
      case Http(e) => f(e)
      case _ => z
    }

  def parseOr[A](z: => A, f: (Response[ByteArray], String) => A): A =
    this match {
      case Parse(res, e) => f(res, e)
      case _ => z
    }

  def decodeOr[A](z: => A, f: (Request, String, CursorHistory, Json) => A): A =
    this match {
      case Decode(r, m, h, s) => f(r, m, h, s)
      case _ => z
    }

  final def fold[A](
    http: Throwable => A,
    parse: (Response[ByteArray], String) => A,
    decode: (Request, String, CursorHistory, Json) => A
  ): A =
    this match {
      case Http(e) =>
        http(e)
      case Parse(res, e) =>
        parse(res, e)
      case Decode(r, m, h, s) =>
        decode(r, m, h, s)
    }
}

object Error {
  final case class Http private[Error] (
    err: Throwable
  )(override val toString: String = "HttpError(" + err + ")"
  ) extends Error(Some(err))
  final case class Parse private[Error] (response: Response[ByteArray], err: String) extends Error{
    override def getMessage = err
  }
  final case class Decode private[Error] (
    req: Request, message: String, history: CursorHistory, sourceJson: Json
  ) extends Error {
    override def toString = Seq(
      "request" -> s"${req.method} ${req.url}",
      "message" -> message,
      "history" -> history,
      "source"  -> sourceJson.pretty(PrettyParams.spaces2)
    ).mkString("DecodeError(",", ",")")
  }

  val http: Throwable => Error = e => Http(e)()
  val httpWithToString: (Throwable, String) => Error = (e, str) => Http(e)(str)
  val parse: (Response[ByteArray], String) => Error = Parse
  val decode: (Request, String, CursorHistory, Json) => Error = Decode
}

