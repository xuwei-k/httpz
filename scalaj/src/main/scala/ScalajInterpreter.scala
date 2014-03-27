package httpz
package scalajhttp

import scalaz.{One => _, Two => _, _}

object ScalajInterpreter extends InterpretersTemplate {

  protected override def request2string(req: Request) =
    ScalajHttp(req).asString

}

