package httpz

package object dispatchclassic {

  implicit def toDispatchActionEOps[E, A](a: ActionE[E, A]) =
    new DispatchActionEOps(a)

  private[this] val userAgentHeader =
    Map("User-Agent" -> "dispatch http client")

  private[dispatchclassic] def request2dispatch(r: Request) = {
    import dispatch.classic._
    val req = url(r.url.toString) <:< (userAgentHeader ++ r.headers) <<? r.params
    r.basicAuth.fold(req){case (user, pass) => req.as(user, pass)}.as_str
  }
}



