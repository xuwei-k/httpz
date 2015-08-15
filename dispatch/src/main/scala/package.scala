package httpz

import java.util.zip.GZIPInputStream
import org.apache.http.Header

package object dispatchclassic {

  implicit def toDispatchActionEOps[E, A](a: ActionE[E, A]) =
    new DispatchActionEOps(a)

  private[this] val userAgentHeader =
    Map("User-Agent" -> "dispatch http client")

  private[this] def convertHeaders(headers: Array[Header]): Map[String, List[String]] = {
    val map = collection.mutable.Map.empty[String, List[String]]
    @annotation.tailrec
    def loop(i: Int): Unit = {
      if(i < headers.length) {
        val h = headers(i)
        val k = h.getName
        val newV: List[String] = h.getElements.map(_.getName)(collection.breakOut)
        map.get(k) match {
          case Some(v) =>
            map += ((k, v ++ newV))
          case None =>
            map += ((k, newV))
        }
        loop(i + 1)
      }
    }
    loop(0)
    map.toMap
  }

  private[dispatchclassic] def request2dispatch(r: Request) = {
    import dispatch.classic._
    val r0 = url(r.url) <:< (userAgentHeader ++ r.headers) <<? r.params
    val req = r.body.fold(r0)(r0 << _)
    Handler(
      r.basicAuth.fold(req){case (user, pass) =>
        req.as(user, pass)
      }.copy(method = r.method)
      ,
      (status, res, entity) => entity match {
        case Some(ent) =>
          val stm = (ent.getContent, ent.getContentEncoding) match {
            case (s, null) => s
            case (s, enc) if enc.getValue == "gzip" => new GZIPInputStream(s)
            case (s, _) => s
          }
          try {
            val body = new ByteArray(Core.inputStream2bytes(stm))
            Response(body, status, convertHeaders(res.getAllHeaders))
          }
          finally {
            stm.close()
          }
        case None =>
          Response(ByteArray.empty, status, convertHeaders(res.getAllHeaders))
      }
    )
  }
}



