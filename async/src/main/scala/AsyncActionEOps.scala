package httpz
package async

final class AsyncActionEOps[E, A](
  val self: ActionE[E, A]
) extends AnyVal with ActionOpsTemplate[E, A] {

  override def interpreter = AsyncInterpreter

}
