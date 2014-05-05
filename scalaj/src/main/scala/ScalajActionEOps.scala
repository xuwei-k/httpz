package httpz
package scalajhttp

final class ScalajActionEOps[E, A](
  val self: ActionE[E, A]
) extends AnyVal with ActionOpsTemplate[E, A] {

  override def interpreter = ScalajInterpreter

}
