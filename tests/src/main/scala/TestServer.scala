package httpz

import unfiltered.request._
import unfiltered.response._
import unfiltered.filter.Plan.Intent

object TestServer extends unfiltered.filter.Plan {

  private[this] def json(string: String) =
    Ok ~> JsonContent ~> ResponseString(quote(string))

  private[this] def randomString =
    scala.util.Random.alphanumeric.take(16).mkString

  val TestParamKey, TestHeaderKey, TestHeaderValue, TestPath,
      TestAuthPath, TestAuthUser, TestAuthPass = randomString

  val TestAuthRes = "auth response " + randomString

  private[this] object TestParam {
    def unapply(p: Params.Map): Option[String] =
      p.get(TestParamKey).flatMap(_.headOption)
  }

  def quote(str: String): String =
    "\"" + str + "\""

  private[this] object TestHeader extends StringHeader(TestHeaderKey)

  private[this] val intentList: List[Intent] = (
    (HEAD -> Ok) :: List(
      GET -> "GET",
      POST -> "POST",
      PUT -> "PUT",
      DELETE -> "DELETE",
      OPTIONS -> "OPTIONS",
      PATCH -> "PATCH",
      TRACE -> "TRACE"
    ).map{ case (k, v) => k -> json(v)}
  ).map{ case (method, res) =>
    { case method(Params(TestParam(d)) & TestHeader(h)) if h == TestHeaderValue =>
      res
    }: Intent
  }

  private[this] object AuthExtractor {
    def unapply[A](req: unfiltered.request.HttpRequest[A]): Boolean =
      BasicAuth(req).forall{ case (user, pass) =>
        user == TestAuthUser && pass == TestAuthPass
      }
  }

  private[this] val basicAuthIntent: Intent = {
    case GET(Path(path) & AuthExtractor())
    if path.drop(1) == TestAuthPath =>
    json(TestAuthRes)
  }

  override val intent: Intent =
    intentList.reduceLeft(_ orElse _) orElse basicAuthIntent
}

