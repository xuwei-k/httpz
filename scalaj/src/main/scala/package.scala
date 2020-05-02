package httpz

package object scalajhttp {

  implicit def toScalajActionEOps[E, A](a: ActionE[E, A]) =
    new ScalajActionEOps(a)

}
