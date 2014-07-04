package httpz

import com.ning.http.client.{Request => NingRequest, _}
import scalaz.concurrent.{Future, Task, Strategy, Promise}
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
      .setParameters(r.params.mapValues(v => singletonList(v): JCollection[String]).asJava)
      .setMethod(r.method)

    r.basicAuth.foreach{ case (user, pass) =>
      builder.setRealm(auth(user, pass))
    }

    r.body.foreach(builder.setBody)

    builder.build
  }

  private def execute(request: NingRequest): Promise[Throwable \/ String] = {
    val client = new AsyncHttpClient
    val promise = Promise.emptyPromise[Throwable \/ String](Strategy.DefaultStrategy)
    val handler = new AsyncCompletionHandler[Unit] {
      def onCompleted(response: Response) =
        try{
          promise.fulfill{
            val body = response.getResponseBody
            client.close()
            \/-(body)
          }
        }catch {
          case e: Throwable =>
            client.close()
            promise.fulfill(-\/(e))
        }
    }
    client.executeRequest(request, handler)
    promise
  }

  private[async] def request2async(r: Request): Task[String] = {
    val req = httpz2ning(r)
    val promise = execute(req)
    Task(promise.get).flatMap{
      a => new Task(Future.now(a))
    }
  }

}
