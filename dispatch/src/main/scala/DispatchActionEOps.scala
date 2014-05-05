package httpz
package dispatchclassic

final class DispatchActionEOps[E, A](
  val self: ActionE[E, A]
) extends AnyVal with ActionOpsTemplate[E, A] {

  override def interpreter = DispatchInterpreter

}
