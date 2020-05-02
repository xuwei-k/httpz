package httpz
package async

import scala.concurrent.Await
import scala.concurrent.duration._

object AsyncInterpreter extends InterpretersTemplate {

  override protected def request2response(req: httpz.Request) = {
    Await.result(request2async(req), 60.seconds)
  }

}
