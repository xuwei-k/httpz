package httpz
package native

final class NativeActionEOps[E, A](
  val self: ActionE[E, A]
) extends AnyVal
    with ActionOpsTemplate[E, A] {

  override def interpreter = NativeInterpreter

}
