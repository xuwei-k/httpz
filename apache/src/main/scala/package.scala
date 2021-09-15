package httpz

package object apachehttp {

  implicit def toApacheActionEOps[E, A](a: ActionE[E, A]): ApacheActionEOps[E, A] =
    new ApacheActionEOps(a)

}
