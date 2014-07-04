package httpz

object Test extends httpz.Tests(
  async.AsyncInterpreter,
  Tests.defaultTestMethods.filterNot(
    Set("TRACE", "PATCH", "DELETE", "OPTIONS") // TODO
  )
)
