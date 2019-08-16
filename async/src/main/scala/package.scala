package httpz

import org.asynchttpclient.{Request => AHCRequest, Response => AHCResponse, _}
import scalaz.concurrent.Task
import scala.collection.JavaConverters._
import java.util.Collections.singletonList
import java.lang.{Iterable => JIterable}
import scalaz._

package object async {

  implicit def toAsyncActionEOps[E, A](a: ActionE[E, A]) =
    new AsyncActionEOps(a)

  private def auth(user: String, password: String) = {
    import org.asynchttpclient.Realm.{Builder, AuthScheme}
    new Builder(user, password)
      .setUsePreemptiveAuth(true)
      .setScheme(AuthScheme.BASIC)
      .build()
  }

  private def httpz2ning(r: Request): AHCRequest = {
    val builder = new RequestBuilder
    builder
      .setUrl(r.url)
      .setHeaders(r.headers.map{case (k, v) => (k: CharSequence) -> (singletonList(v): JIterable[String])}.asJava) // TODO
      .setQueryParams(r.params.mapValues(v => singletonList(v)).toMap.asJava)
      .setMethod(r.method)

    r.basicAuth.foreach{ case (user, pass) =>
      builder.setRealm(auth(user, pass))
    }

    r.body.foreach(builder.setBody)

    builder.build
  }


  private def execute(request: AHCRequest): Task[Response[ByteArray]] = {
    val config = new DefaultAsyncHttpClientConfig.Builder()
      .build()
    val client = new DefaultAsyncHttpClient(config)
    Task.async[Response[ByteArray]]{ function =>
      val handler = new AsyncCompletionHandler[Unit] {
        def onCompleted(res: AHCResponse) =
          try{
            val body = new ByteArray(res.getResponseBodyAsBytes)
            val status = res.getStatusCode
            val rawHeaders = res.getHeaders
            val headerKeys = rawHeaders.asScala.map(_.getKey)
            val headers = headerKeys.iterator.map(key => key -> rawHeaders.getAll(key).asScala.toList).toMap
            client.close()
            function(\/-(Response(body, status, headers)))
          }catch {
            case e: Throwable =>
              client.close()
              function(-\/(e))
          }
      }
      client.executeRequest(request, handler)
    }
  }

  private[async] def request2async(r: Request): Task[Response[ByteArray]] = {
    val req = httpz2ning(r)
    execute(req)
  }

}
