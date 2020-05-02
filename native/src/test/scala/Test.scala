package httpz

object Test
    extends httpz.Tests(
      native.NativeInterpreter,
      Tests.defaultTestMethods.filterNot(
        Set("PATCH")
      )
    )
