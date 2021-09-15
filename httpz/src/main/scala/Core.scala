package httpz

import java.io.{ByteArrayOutputStream, InputStream}

import scalaz._
import argonaut._

object Core extends Core[RequestF] {

  /**
   * @see [[https://dl.dropboxusercontent.com/u/4588997/ReasonablyPriced.pdf]]
   * @see [[https://gist.github.com/runarorama/a8fab38e473fafa0921d]]
   */
  implicit def instance[F[_]](implicit I: Inject[RequestF, F]): Core[F] =
    new Core[F]

  def inputStream2bytes(in: InputStream): Array[Byte] = {
    val buf = new ByteArrayOutputStream()
    val data = new Array[Byte](4096)
    @annotation.tailrec
    def loop(): Unit = {
      in.read(data, 0, data.length) match {
        case n if n > 0 =>
          buf.write(data, 0, n)
          loop()
        case _ =>
      }
    }
    loop()
    buf.toByteArray
  }
}

sealed class Core[F[_]](implicit I: Inject[RequestF, F]) {

  private[this] def lift[A, B](f: RequestF[A \/ B]) =
    EitherT[A, ({ type l[a] = Free[F, a] })#l, B](Free.liftF(I.inj(f)))

  def json[A](req: Request)(implicit
    A: DecodeJson[A]
  ): EitherT[Error, ({ type l[a] = Free[F, a] })#l, A] =
    jsonResponse[A](req).map(_.body)

  def jsonResponse[A](req: Request)(implicit
    A: DecodeJson[A]
  ): EitherT[Error, ({ type l[a] = Free[F, a] })#l, Response[A]] =
    lift(
      RequestF.one[Error \/ Response[A], Error \/ Response[Json]](
        req,
        \/.left,
        (request, response) => {
          val str = response.bodyUTF8
          Parse.parse(str) match {
            case Right(json) =>
              \/-(response.copy(body = json))
            case Left(e) =>
              -\/(Error.parse(response, e))
          }
        },
        (request, either) =>
          either.flatMap { json =>
            A.decodeJson(json.body).result match {
              case Right(r) => \/-(json.copy(body = r))
              case Left((msg, history)) => -\/(Error.decode(request, msg, history, json.body))
            }
          }
      )
    )

  def raw(
    req: Request
  ): EitherT[Throwable, ({ type l[a] = Free[F, a] })#l, Response[ByteArray]] =
    lift(
      RequestF.one[Throwable \/ Response[ByteArray], Response[ByteArray]](
        req,
        \/.left,
        (_, response) => response,
        (_, result) => \/-(result)
      )
    )

  def bytes(
    req: Request
  ): EitherT[Throwable, ({ type l[a] = Free[F, a] })#l, ByteArray] =
    raw(req).map(_.body)

  def string(
    req: Request
  ): EitherT[Throwable, ({ type l[a] = Free[F, a] })#l, String] =
    stringResponse(req).map(_.body)

  def stringResponse(
    req: Request
  ): EitherT[Throwable, ({ type l[a] = Free[F, a] })#l, Response[String]] =
    lift(
      RequestF.one[Throwable \/ Response[String], Response[String]](
        req,
        \/.left,
        (_, response) => response.asUTF8StringBody,
        (_, result) => \/-(result)
      )
    )
}
