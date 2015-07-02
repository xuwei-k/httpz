package httpz
package scalajhttp

object ScalajInterpreter extends InterpretersTemplate {

  protected override def request2string(req: Request) =
    ScalajHttp(req).asString.body

}
