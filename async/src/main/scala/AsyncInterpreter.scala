package httpz
package async

import scalaz.concurrent.Task

object AsyncInterpreter extends InterpretersTemplate {

  override protected def request2response(req: httpz.Request) = {
    request2async(req).unsafePerformSync
  }

  override protected def task[A](one: httpz.RequestF.One[A], conf: Config): Task[A] = {
    val req = conf(one.req)
    request2async(req).map{ result =>
      one.decode(req, one.parse(req, result))
    }
  }

}
