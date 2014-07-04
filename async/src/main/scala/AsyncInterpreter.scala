package httpz
package async

import scalaz.concurrent.Task

object AsyncInterpreter extends InterpretersTemplate {

  override protected def request2string(req: httpz.Request): String = {
    request2async(req).run
  }

  override protected def task[A](one: httpz.RequestF.One[A], conf: Config): Task[A] = {
    val req = conf(one.req)
    request2async(req).map{ result =>
      one.decode(req, one.parse(req, result))
    }
  }

}
