package httpz

import argonaut.EncodeJson

import scalaz._

final case class Response[A](body: A, status: Int, headers: Map[String, List[String]]) {
  def map[B](f: A => B): Response[B] =
    copy(f(body))

  def bodyString(charset: String)(implicit A: A =:= ByteArray): String =
    new String(A(body).value, charset)

  def bodyUTF8(implicit A: A =:= ByteArray): String =
    bodyString("UTF-8")

  def asStringBody(charset: String)(implicit A: A =:= ByteArray): Response[String] =
    map(_ => bodyString(charset))

  def asUTF8StringBody(implicit A: A =:= ByteArray): Response[String] =
    map(_ => bodyUTF8)

  def ===(that: Response[A])(implicit A: Equal[A]): Boolean =
    (this.status == that.status) && (this.headers == that.headers) && A.equal(this.body, that.body)

  override def toString: String =
    Response.codecAny.encode(this.asInstanceOf[Response[Any]]).toString()
}

object Response {
  private[this] val any2jStringEncoder: EncodeJson[Any] =
    EncodeJson(value => argonaut.Json.jString(value.toString))

  private val codecAny: EncodeJson[Response[Any]] =
    codec(any2jStringEncoder)

  implicit def codec[A: EncodeJson]: EncodeJson[Response[A]] =
    EncodeJson.jencode3L((res: Response[A]) => (res.body, res.status, res.headers))("body", "status", "headers")

  implicit val instance: Functor[Response] =
    new Functor[Response] {
      def map[A, B](fa: Response[A])(f: A => B) =
        fa map f
    }

  implicit def responseEqual[A: Equal]: Equal[Response[A]] =
    Equal.equal(_ === _)
}
