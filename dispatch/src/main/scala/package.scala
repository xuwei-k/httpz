package httpz

package object dispatchclassic {

  implicit def toDispatchActionEOps[E, A](a: ActionE[E, A]) =
    new DispatchActionEOps(a)

  private[this] val userAgentHeader =
    Map("User-Agent" -> "dispatch http client")

  private[dispatchclassic] def request2dispatch(r: Request) = {
    import dispatch.classic._
    val r0 = url(r.url) <:< (userAgentHeader ++ r.headers) <<? r.params
    val req = r.body.fold(r0)(r0 << _)
    r.basicAuth.fold(req){case (user, pass) =>
      req.as(user, pass)
    }.copy(method = r.method).as_str
  }
}



