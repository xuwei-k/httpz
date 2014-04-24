package httpz

import Macros._
import argonaut._

object Httpbin {
  final case class HttpbinGet(
    origin: String,
    headers: Map[String, String],
    url: String,
    args: Map[String, String]
  ) extends JsonToString[HttpbinGet]

  object HttpbinGet {
    implicit val instance: CodecJson[HttpbinGet] =
      CodecJson.casecodec4(apply, unapply)(
        "origin", "headers", "url", "args"
      )  
  }
  
  def get[A: DecodeJson](path: String, conf: Config = emptyConfig): Action[A] =
    Core.json(conf(Request("http://httpbin.org/" + path)))

  def check(interpreter: InterpretersTemplate): Unit = {
    import interpreter.sequential.empty.run
        
    assertEqual(run(get[HttpbinGet]("get")).isRight, true)
  }

}
