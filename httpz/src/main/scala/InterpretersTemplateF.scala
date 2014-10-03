package httpz

import scalaz.Id.Id
import scalaz.concurrent.{Future, Task}
import scalaz.~>

trait Template[F[_], G[_]] { self =>
  val empty: NaturalTransMonad[F, G]
  def apply(conf: Config): NaturalTransMonad[F, G]
  def contramap[F0[_]](f: F0 ~> F): Template[F0, G] =
    new Template[F0, G] {
      override val empty =
        self.empty.contramap(f)
      override def apply(conf: Config) =
        self.apply(conf).contramap(f)
    }
}

trait InterpretersTemplateF[F[_]] { self =>

  protected[this] type T[G[_]] = Template[F, G]

  val future: T[Future]
  val task: T[Task]
  val sequential: T[Id]
  val times: T[Times]
  val futureTimes: T[FutureTimes]

  def contramap[F0[_]](f: F0 ~> F): InterpretersTemplateF[F0] =
    new InterpretersTemplateF[F0] {
      override val future = self.future.contramap(f)
      override val task = self.task.contramap(f)
      override val sequential = self.sequential.contramap(f)
      override val times = self.times.contramap(f)
      override val futureTimes = self.futureTimes.contramap(f)
    }
}
