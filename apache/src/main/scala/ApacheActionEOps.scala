package httpz
package apachehttp

final class ApacheActionEOps[E, A](
  val self: ActionE[E, A]
) extends AnyVal with ActionOpsTemplate[E, A] {

  override def interpreter = ApacheInterpreter

}
