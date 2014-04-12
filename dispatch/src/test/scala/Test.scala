package httpz

object Test extends httpz.Tests(
  dispatchclassic.DispatchInterpreter,
  Tests.defaultTestMethods.filterNot(
    Set("TRACE", "PATCH")
  )
)

