package httpz
package dispatchclassic

import scalaz.{One => _, Two => _, _}

final class DispatchActionEOps[E, A](
  val self: ActionE[E, A]
) extends AnyVal with ActionOpsTemplate[E, A] {

  override def interpreter = DispatchInterpreter

}
