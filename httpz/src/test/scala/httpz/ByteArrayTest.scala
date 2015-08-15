package httpz

import scalaprops._

object ByteArrayTest extends Scalaprops {

  implicit val gen: Gen[ByteArray] =
    Gen[Array[Byte]].map(new ByteArray(_))

  val laws = scalazlaws.order.all[ByteArray]

}
