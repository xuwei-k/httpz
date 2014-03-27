package httpz

import scalaz.{One => _, Two => _, _}
import scalaz.concurrent.{Future, Task}
import Z._

trait ActionOpsTemplate[E, A] extends Any {

  protected[this] def interpreter: InterpretersTemplate

  def self: ActionE[E, A]

  def task: Task[E \/ A] =
    interpreter.task.empty.run(self)

  def task(conf: Config): Task[E \/ A] =
    interpreter.task.apply(conf).run(self)

  def async: Future[E \/ A] =
    interpreter.future.empty.run(self)

  def async(conf: Config): Future[E \/ A] =
    interpreter.future.apply(conf).run(self)

  def withTime: Times[E \/ A] =
    interpreter.times.empty.run(self)

  def withTime(conf: Config): Times[E \/ A] =
    interpreter.times.apply(conf).run(self)

  def futureWithTime: Future[(List[Time], E \/ A)] =
    interpreter.times.future.empty.run(self).run

  def futureWithTime(conf: Config): Future[(List[Time], E \/ A)] =
    interpreter.times.future(conf).run(self).run

  def interpret: E \/ A =
    interpreter.sequential.empty.run(self)

  def interpretWith(conf: Config): E \/ A =
    interpreter.sequential.apply(conf).run(self)

}
