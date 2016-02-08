package httpz

object Test extends httpz.Tests(
  interpreter = scalajhttp.ScalajInterpreter,
  headerType = HeaderType.Multi
)

