package httpz

import scalaz._
import argonaut._

object Core {

  def json[A](req: Request)(implicit A: DecodeJson[A]): Action[A] =
    Action(Free.liftFC(RequestF.one[Error \/ A, Error \/ Json](
      req,
      \/.left,
      (request, result) => Parse.parse(result).leftMap(Error.parse),
      (request, either) => either.flatMap{ json =>
        A.decodeJson(json).result match {
           case r @ \/-(_) => r
           case -\/((msg, history)) => -\/(Error.decode(request, msg, history, json))
        }
      }
    )))

  def string(req: Request): ActionE[Throwable, String] =
    Action(Free.liftFC(RequestF.one[Throwable \/ String, String](
      req,
      \/.left,
      (_, result) => result,
      (_, result) => \/-(result)
   )))

}

