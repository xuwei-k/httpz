package httpz

package object native {

  implicit def toNativeActionEOps[E, A](a: ActionE[E, A]): NativeActionEOps[E, A] =
    new NativeActionEOps(a)

}
