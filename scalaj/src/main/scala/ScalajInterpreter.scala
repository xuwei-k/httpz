package httpz
package scalajhttp

object ScalajInterpreter extends InterpretersTemplate {

  protected override def request2response(req: Request) = {
    val res = ScalajHttp(req).asBytes
    Response(new ByteArray(res.body), res.code, res.headers.mapValues(_ :: Nil))
  }

}
