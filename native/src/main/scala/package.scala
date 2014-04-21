package httpz

import scalaz.{One => _, Two => _, _}

package object native {

  implicit def toNativeActionEOps[E, A](a: ActionE[E, A]) =
    new NativeActionEOps(a)

}

