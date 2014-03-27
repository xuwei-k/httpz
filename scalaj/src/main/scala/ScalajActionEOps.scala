package httpz
package scalajhttp

import scalaz.{One => _, Two => _, _}

final class ScalajActionEOps[E, A](
  val self: ActionE[E, A]
) extends AnyVal with ActionOpsTemplate[E, A] {

  override def interpreter = ScalajInterpreter

}
