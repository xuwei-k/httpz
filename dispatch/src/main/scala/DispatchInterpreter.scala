package httpz
package dispatchclassic

object DispatchInterpreter extends InterpretersTemplate {

  override protected def request2string(req: httpz.Request) = {
    import dispatch.classic._
    Http(request2dispatch(req))
  }

}

