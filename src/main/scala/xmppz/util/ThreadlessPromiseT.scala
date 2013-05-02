package xmppz.util

import scala.concurrent.{ Future, ExecutionContext }
import ExecutionContext.Implicits.global
import scalaz._
import Scalaz._

//doesn't work with all other monads or monadts, may be wrong, not using for now..maybe Future just can't be in the stakc unless it's at the bottom...
case class PromiseT[F[+_], +A](val run: F[Future[A]]) {

  def map[B](f: A => B)(implicit functor: Functor[F]) = new PromiseT[F, B](
    functor.map(run)(future =>
      future.map(i =>
        f(i)
      )
    )
  )

  def flatMap[B](f: A => PromiseT[F, B])(implicit monad: Monad[F]): PromiseT[F, B] = {
    val newp = new ThreadlessPromise[B]
    val ret = monad.bind(run)(future => {
      future.map(a => {
        f(a).map(b => { //should we do something else here?
          newp.success(b)
        })
      })
      monad.point(newp.future)
    })
    PromiseT(ret)
  }
}

//this is how scalaz handles putting MonadTs into other things that take Monad, so
//i'll follow there example in case we want to have conversions to other typeclasses. 
//no unicode varialbes though...

object PromiseT {
  implicit def PromiseTMonad[F[+_]](implicit F0: Monad[F]) = new PromiseTMonad[F] {
    implicit def F: Monad[F] = F0
  }

}

trait PromiseTMonad[F[+_]] extends Monad[({ type L[a] = PromiseT[F, a] })#L] {
  implicit def F: Monad[F]

  def point[A](a: => A): PromiseT[F, A] = PromiseT[F, A](F.point(scala.concurrent.Future.successful(a)))

  def bind[A, B](p: PromiseT[F, A])(f: A => PromiseT[F, B]): PromiseT[F, B] = p.flatMap(f(_))

}

