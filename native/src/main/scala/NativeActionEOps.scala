package httpz
package native

import scalaz.{One => _, Two => _, _}

final class NativeActionEOps[E, A](
  val self: ActionE[E, A]
) extends AnyVal with ActionOpsTemplate[E, A] {

  override def interpreter = NativeInterpreter

}
