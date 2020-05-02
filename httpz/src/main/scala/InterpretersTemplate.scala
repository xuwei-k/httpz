package httpz

import scalaz._
import scalaz.Id.Id
import scalaz.std.scalaFuture._

import scala.concurrent.{ExecutionContext, Future}
import RequestF._

abstract class InterpretersTemplate {

  protected[this] def request2response(req: Request): Response[ByteArray]

  private def runOne[A](o: One[A], conf: Config): A =
    try {
      val r = conf(o.req)
      o.decode(r, o.parse(r, request2response(r)))
    } catch {
      case e: Throwable =>
        onHttpError(o, e)
    }

  protected[this] def onHttpError[A](o: One[A], e: Throwable): A =
    o.error(Error.http(e))

  object future {
    def empty(implicit e: ExecutionContext): Interpreter[Future] =
      apply(emptyConfig)

    def apply(conf: Config)(implicit e: ExecutionContext): Interpreter[Future] =
      new Interpreter[Future] {
        def go[A](a: RequestF[A]) =
          a match {
            case o @ One() =>
              Future(runOne(o, conf))
            case t @ Two() =>
              Nondeterminism[Future].mapBoth(run(t.x), run(t.y))(t.f)
          }
      }
  }

  object sequential {
    val empty: Interpreter[Id] =
      apply(emptyConfig)

    def apply(conf: Config): Interpreter[Id] =
      new Interpreter[Id] {
        def go[A](a: RequestF[A]) =
          a match {
            case o @ One() =>
              runOne(o, conf)
            case t @ Two() =>
              t.f(run(t.x), run(t.y))
          }
      }
  }

  object times {
    val empty: Interpreter[Times] =
      apply(emptyConfig)

    def apply(conf: Config): Interpreter[Times] =
      new Interpreter[Times] {
        def go[A](a: RequestF[A]) =
          a match {
            case o @ One() =>
              Times(go1(o, conf))
            case t @ Two() =>
              timesMonad.apply2(run(t.x), run(t.y))(t.f)
          }
      }

    private def go1[A](o: One[A], conf: Config): (List[Time], A) = {
      val r = conf(o.req)
      val start = System.nanoTime
      try {
        val res = request2response(r)
        val httpFinished = System.nanoTime
        val parseResult = o.parse(r, res)
        val parseFinished = System.nanoTime
        val decodeResult = o.decode(r, parseResult)
        val decodeFinished = System.nanoTime - parseFinished
        val time = Time.Success(
          r,
          res,
          httpFinished - start,
          parseFinished - httpFinished,
          decodeFinished
        )
        (time :: Nil) -> decodeResult
      } catch {
        case e: Throwable =>
          (Time.Failure(r, e, System.nanoTime - start) :: Nil) -> o.error(Error.http(e))
      }
    }

    object future {
      private[this] def FutureApParallel(implicit e: ExecutionContext) =
        new Applicative[Future] {
          override def point[A](a: => A) = Future(a)
          override def ap[A, B](a: => Future[A])(f: => Future[A => B]): Future[B] = apply2(f, a)(_(_))
          override def apply2[A, B, C](a: => Future[A], b: => Future[B])(f: (A, B) => C) =
            Nondeterminism[Future].mapBoth(a, b)(f)
        }
      import std.list._

      private[this] def G(implicit e: ExecutionContext) =
        WriterT.writerTApplicative(Monoid[List[Time]], FutureApParallel)

      def empty(implicit e: ExecutionContext): Interpreter[FutureTimes] =
        apply(emptyConfig)

      def apply(conf: Config)(implicit e: ExecutionContext): Interpreter[FutureTimes] =
        new Interpreter[FutureTimes] {
          def go[A](a: RequestF[A]) =
            a match {
              case o @ One() =>
                WriterT(Future(go1(o, conf)))
              case t @ Two() =>
                G.apply2(run(t.x), run(t.y))(t.f)
            }
        }
    }
  }
}
