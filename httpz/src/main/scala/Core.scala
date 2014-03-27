package httpz

import scalaz._
import argonaut._

object Core {

  def httpRequest[A](req: Request)(implicit A: DecodeJson[A]): Action[A] =
    Action(Z.freeC(RequestF.one[Error \/ A, Error \/ Json](
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

}

