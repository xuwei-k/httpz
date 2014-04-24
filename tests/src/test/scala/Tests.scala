package httpz

import argonaut.Json

abstract class Tests(
  interpreter: InterpretersTemplate,
  methods: List[String] = Tests.defaultTestMethods
){
  final def main(args: Array[String]): Unit = {
    Tests.test(interpreter, methods)
    Httpbin.check(interpreter)
  }
}

object Tests {

  val defaultTestMethods = List(
    "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "TRACE"//, "HEAD" // TODO
  )

  private def test(interpreter: InterpretersTemplate, httpMethods: List[String]) =
    useTestServer{ server =>
      runTest(interpreter, server, httpMethods)
    }

  private def useTestServer[A](function: unfiltered.jetty.Http => A): A = {
    val server = unfiltered.jetty.Http.anylocal
    server.filter(TestServer).start
    try{
      function(server)
    } finally {
      server.stop
      server.destroy
    }
  }

  private def runTest(
    interpreter: InterpretersTemplate,
    server: unfiltered.jetty.Http,
    methods: List[String]
  ): Unit = {
    val url = s"http://localhost:${server.port}/${TestServer.TestPath}"
    val func: String => Action[Json] = { str =>
      Core.json(Request(
        method = "dummy",
        url = url,
        params = Map(TestServer.TestParamKey -> str),
        headers = Map(
          TestServer.TestHeaderKey -> TestServer.TestHeaderValue
        )
      ))
    }

    val testParams = Seq("{}")

    val basicAuthReq = {
      import TestServer._
      val method = "GET"
      quote(TestAuthRes) -> Core.json(Request(
        url = s"http://localhost:${server.port}/${TestServer.TestAuthPath}",
        method = method,
        basicAuth = Some((TestAuthUser, TestAuthPass))
      ))
    }

    for{
      p <- testParams
      (expect, action) <- basicAuthReq :: methods.map{ method =>
        TestServer.quote(method) -> func(p).mapRequest(Request.method(method))
      }
    }{
      val result = interpreter.sequential.empty.run(action)
      println(result)
      import scalaz.{\/-, -\/}
      result match {
        case \/-(a) =>
          assert(a.toString == expect, s"$a is not equal to $expect, $interpreter")
        case -\/(e) =>
          throw e
      }
    }
  }

}
