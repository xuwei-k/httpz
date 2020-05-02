package httpz

import java.util.Arrays

import scalaz.{Order, Ordering}

final class ByteArray(val value: Array[Byte]) {
  override def toString: String = hexString("ByteArray(size = " + value.length + " value = ", " ", ")", 4)

  def hexString(start: String, sep: String, end: String, n: Int): String = {
    value.sliding(n, n).map(_.map(x => "%02x".format(x & 0xff)).mkString).mkString(start, sep, end)
  }

  def ===(that: ByteArray): Boolean = {
    if (this eq that)
      true
    else
      Arrays.equals(this.value, that.value)
  }

  override def equals(other: Any): Boolean =
    other match {
      case that: ByteArray =>
        this.===(that)
      case _ =>
        false
    }

  override def hashCode: Int = Arrays.hashCode(value)
}

object ByteArray {
  val empty: ByteArray = new ByteArray(Array.empty[Byte])

  implicit val instance: Order[ByteArray] =
    new Order[ByteArray] {
      import scalaz.std.anyVal._
      override def equal(x: ByteArray, y: ByteArray) =
        x === y
      override def order(x: ByteArray, y: ByteArray) =
        Order[Int].order(x.value.length, y.value.length) match {
          case Ordering.EQ =>
            @annotation.tailrec
            def loop(i: Int): Ordering = {
              if (i >= x.value.length) {
                Ordering.EQ
              } else if (x.value(i) < y.value(i)) {
                Ordering.LT
              } else if (x.value(i) > y.value(i)) {
                Ordering.GT
              } else {
                loop(i + 1)
              }
            }
            loop(0)
          case other =>
            other
        }

    }
}
