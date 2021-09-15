package httpz

import scalaprops._
import scalaz.std.anyVal._

object ResponseTest extends Scalaprops {
  private[this] implicit val strGen: Gen[String] = Gen.asciiString

  implicit def gen[A: Gen]: Gen[Response[A]] =
    Gen.from3(Response.apply[A] _)

  val laws = Properties.list(
    scalazlaws.equal.all[Response[Byte]],
    scalazlaws.functor.all[Response]
  )

}
