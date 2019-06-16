package httpz

import argonaut.Json
import scalaz.{\/-, -\/}

abstract class Tests(
  interpreter: InterpretersTemplate,
  methods: List[String] = Tests.defaultTestMethods,
  headerType: HeaderType = HeaderType.Multi
){
  final def main(args: Array[String]): Unit = Tests.test(interpreter, methods, headerType)
}

object Tests {

  val defaultTestMethods = List(
    "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "TRACE"//, "HEAD" // TODO
  )

  private def test(interpreter: InterpretersTemplate, httpMethods: List[String], headerType: HeaderType) =
    useTestServer{ server =>
      runTest(interpreter, server, httpMethods, headerType)
    }

  private def useTestServer[A](function: unfiltered.jetty.Server => A): Unit = {
    val server = unfiltered.jetty.Server.anylocal.plan(TestServer)
    server.start()
    try{
      function(server)
    } finally {
      server.stop()
    }
  }

  private def runTest(
    interpreter: InterpretersTemplate,
    server: unfiltered.jetty.Server,
    methods: List[String],
    headerType: HeaderType
  ): Unit = {
    val port = server.ports.headOption.getOrElse(sys.error("empty port binding!?"))
    val url = s"http://localhost:${port}/${TestServer.TestPath}"
    val funcJson: String => Action[Json] = { str =>
      Core.json(Request(
        method = "dummy",
        url = url,
        params = Map(TestServer.TestParamKey -> str),
        headers = Map(
          TestServer.TestHeaderKey -> TestServer.TestHeaderValue
        )
      ))
    }

    val funcRaw = Core.raw(Request(
      method = "dummy",
      url = url,
      params = Map(TestServer.TestParamKey -> "{}"),
      headers = Map(
        TestServer.TestHeaderKey -> TestServer.TestHeaderValue
      )
    )).map(_.headers)

    val testParams = Seq("{}")

    val basicAuthReq = {
      import TestServer._
      val method = "GET"
      quote(TestAuthRes) -> Core.json[Json](Request(
        url = s"http://localhost:${port}/${TestServer.TestAuthPath}",
        method = method,
        basicAuth = Some((TestAuthUser, TestAuthPass))
      ))
    }


    methods.foreach{ method =>
      val expect = TestServer.testResponseHeaders
      val action = funcRaw.mapRequest(Request.method(method))
      val result = interpreter.sequential.empty.run(action)
      println(result)
      result match {
        case \/-(a) =>
          expect.foreach{ case (k, v) =>
            headerType match {
              case _: HeaderType.CommaSeparated.type =>
                assert(a.get(k).map(_.head) == Some(v.mkString(", ")), List(k, a, expect).mkString("\n", "\n", ""))
              case _: HeaderType.Multi.type =>
                assert(a.get(k) == Some(v), List(k, a, expect).mkString("\n", "\n", ""))
            }
          }
        case -\/(e) =>
          throw e
      }
    }

    for{
      p <- testParams
      (expect, action) <- basicAuthReq :: methods.map{ method =>
        TestServer.quote(method) -> funcJson(p).mapRequest(Request.method(method))
      }
    }{
      val result = interpreter.sequential.empty.run(action)
      println(result)
      result match {
        case \/-(a) =>
          assert(a.toString == expect, s"$a is not equal to $expect, $interpreter")
        case -\/(e) =>
          throw e
      }
    }
  }

}
