package httpz

import com.ning.http.client.{Request => NingRequest, Response => NingResponse, _}
import scalaz.concurrent.Task
import scala.collection.convert.decorateAsJava._
import java.util.Collections.singletonList
import java.util.{Collection => JCollection}
import scalaz._

package object async {

  implicit def toAsyncActionEOps[E, A](a: ActionE[E, A]) =
    new AsyncActionEOps(a)

  private def auth(user: String, password: String) = {
     import com.ning.http.client.Realm.{RealmBuilder,AuthScheme}
     new RealmBuilder()
       .setPrincipal(user)
       .setPassword(password)
       .setUsePreemptiveAuth(true)
       .setScheme(AuthScheme.BASIC)
       .build()
  }

  private def httpz2ning(r: Request): NingRequest = {
    val builder = new RequestBuilder
    builder
      .setUrl(r.url)
      .setHeaders(r.headers.mapValues(v => singletonList(v): JCollection[String]).asJava) // TODO
      .setQueryParams(r.params.mapValues(v => singletonList(v)).asJava)
      .setMethod(r.method)

    r.basicAuth.foreach{ case (user, pass) =>
      builder.setRealm(auth(user, pass))
    }

    r.body.foreach(builder.setBody)

    builder.build
  }


  private def execute(request: NingRequest): Task[Response[ByteArray]] = {
    val config = new AsyncHttpClientConfig.Builder()
      .setIOThreadMultiplier(1)
      .build()
    val client = new AsyncHttpClient(config)
    Task.async[Response[ByteArray]]{ function =>
      val handler = new AsyncCompletionHandler[Unit] {
        def onCompleted(res: NingResponse) =
          try{
            import scala.collection.JavaConverters._
            val body = new ByteArray(res.getResponseBodyAsBytes)
            val status = res.getStatusCode
            val headers = mapAsScalaMapConverter(res.getHeaders).asScala.mapValues(_.asScala.toList).toMap
            client.closeAsynchronously()
            function(\/-(Response(body, status, headers)))
          }catch {
            case e: Throwable =>
              client.closeAsynchronously()
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
