package httpz
package apachehttp

import java.io.Closeable
import java.net.URI
import java.util.zip.GZIPInputStream

import org.apache.http.Header
import org.apache.http.auth.UsernamePasswordCredentials
import org.apache.http.client.methods._
import org.apache.http.client.protocol.HttpClientContext
import org.apache.http.entity.ByteArrayEntity
import org.apache.http.impl.auth.BasicScheme
import org.apache.http.impl.client.HttpClients

final case class HttpError(
  status: Int,
  body: String
) extends RuntimeException {
  override def toString =
    Seq(
      "status" -> status,
      "body" -> body
    ).mkString(getClass.getName + "(", ", ", ")")
}

object ApacheInterpreter extends InterpretersTemplate {

  private def using[A <: Closeable, B](resource: A)(f: A => B): B =
    try {
      f(resource)
    } finally {
      resource.close()
    }

  private def setByteArrayEntity(req: HttpEntityEnclosingRequestBase, body: Option[Array[Byte]]) = {
    body match {
      case Some(bytes) =>
        req.setEntity(new ByteArrayEntity(bytes))
      case None =>
    }
    req
  }

  private def executeRequest(req: httpz.Request) = {
    val uri = buildURI(req)
    val request = req.method match {
      case "GET" =>
        new HttpGet(uri)
      case "HEAD" =>
        new HttpHead(uri)
      case "DELETE" =>
        new HttpDelete(uri)
      case "OPTIONS" =>
        new HttpOptions(uri)
      case "TRACE" =>
        new HttpTrace(uri)
      case other =>
        val r = other match {
          case "PUT" => new HttpPut(uri)
          case "PATCH" => new HttpPatch(uri)
          case "POST" => new HttpPost(uri)
        }
        setByteArrayEntity(r, req.body)
    }
    val c = HttpClients.createDefault()
    req.basicAuth.foreach { case (user, pass) =>
      val creds = new UsernamePasswordCredentials(user, pass)
      val context = HttpClientContext.create()
      request.addHeader(new BasicScheme().authenticate(creds, request, context))
    }
    req.headers.foreach { case (k, v) => request.addHeader(k, v) }
    c.execute(request)
  }

  private[this] def convertHeaders(headers: Array[Header]): Map[String, List[String]] = {
    val map = collection.mutable.Map.empty[String, List[String]]
    @annotation.tailrec
    def loop(i: Int): Unit = {
      if (i < headers.length) {
        val h = headers(i)
        val k = h.getName
        val newV: List[String] = h.getElements.map(_.getName).toList
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

  override protected def request2response(req: httpz.Request) = {
    using(executeRequest(req)) { res =>
      val code = res.getStatusLine().getStatusCode()
      val headers = convertHeaders(res.getAllHeaders())
      val entity = res.getEntity()
      val stm = (entity.getContent, entity.getContentEncoding) match {
        case (s, null) => s
        case (s, enc) if enc.getValue == "gzip" => new GZIPInputStream(s)
        case (s, _) => s
      }
      val body =
        try {
          new ByteArray(Core.inputStream2bytes(stm))
        } finally {
          stm.close()
        }
      Response(body, code, headers)
    }
  }

  private def buildURI(req: httpz.Request): URI = {
    val uriBuilder = new org.apache.http.client.utils.URIBuilder(req.url)
    req.params.foreach { case (key, value) =>
      uriBuilder.addParameter(key, value)
    }
    uriBuilder.build
  }

}
