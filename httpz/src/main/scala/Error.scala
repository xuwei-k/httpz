package httpz

import argonaut._

sealed trait Error extends RuntimeException with Product with Serializable {
  import Error._

  def httpOr[A](z: => A, f: Throwable => A): A =
    this match {
      case Http(e) => f(e)
      case _ => z
    }

  def parseOr[A](z: => A, f: String => A): A =
    this match {
      case Parse(e) => f(e)
      case _ => z
    }

  def decodeOr[A](z: => A, f: (Request, String, CursorHistory, Json) => A): A =
    this match {
      case Decode(r, m, h, s) => f(r, m, h, s)
      case _ => z
    }

  final def fold[A](
    http: Throwable => A,
    parse: String => A,
    decode: (Request, String, CursorHistory, Json) => A
  ): A =
    this match {
      case Http(e) =>
        http(e)
      case Parse(e) =>
        parse(e)
      case Decode(r, m, h, s) =>
        decode(r, m, h, s)
    }
}

object Error {
  final case class Http private[Error] (err: Throwable) extends Error {
    override def toString = "HttpError(" + err + ")"
  }
  final case class Parse private[Error] (err: String) extends Error
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

  val http: Throwable => Error = Http
  val parse: String => Error = Parse
  val decode: (Request, String, CursorHistory, Json) => Error = Decode
}

