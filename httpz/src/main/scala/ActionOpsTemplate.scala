package httpz

import scalaz._

import scala.concurrent.{ExecutionContext, Future}

trait ActionOpsTemplate[E, A] extends Any {

  protected[this] def interpreter: InterpretersTemplate

  def self: ActionE[E, A]

  def async(implicit e: ExecutionContext): Future[E \/ A] =
    interpreter.future.empty.run(self)

  def async(conf: Config)(implicit e: ExecutionContext): Future[E \/ A] =
    interpreter.future.apply(conf).run(self)

  def withTime: Times[E \/ A] =
    interpreter.times.empty.run(self)

  def withTime(conf: Config): Times[E \/ A] =
    interpreter.times.apply(conf).run(self)

  def futureWithTime(implicit e: ExecutionContext): Future[(List[Time], E \/ A)] =
    interpreter.times.future.empty.run(self).run

  def futureWithTime(conf: Config)(implicit e: ExecutionContext): Future[(List[Time], E \/ A)] =
    interpreter.times.future(conf).run(self).run

  def interpret: E \/ A =
    interpreter.sequential.empty.run(self)

  def interpretWith(conf: Config): E \/ A =
    interpreter.sequential.apply(conf).run(self)

}
