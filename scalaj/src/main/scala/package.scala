package httpz

import scalaz.{One => _, Two => _, _}

package object scalajhttp {

  implicit def toScalajActionEOps[E, A](a: ActionE[E, A]) =
    new ScalajActionEOps(a)

}

