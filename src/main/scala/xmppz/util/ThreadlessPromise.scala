package xmppz.util

import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConversions._
import scala.collection.JavaConversions
import java.util.Queue
import scala.concurrent.CanAwait
import scala.util.{ Try, Success }
import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future, Promise }
import ExecutionContext.Implicits.global

//the normal promise shoots off the 'fulfillment' part to a different thread, we don't want that
//TODO: may need to make this trampoline like ScalaZ's Future
class ThreadlessPromise[A] extends Future[A] {

  private val q: ConcurrentLinkedQueue[A => Unit] = new ConcurrentLinkedQueue[A => Unit]()

  def future: Future[A] = this

  private var successfulVal: Option[A] = None

  def success(a: A): Unit = {
    successfulVal = Some(a)
    q.foreach(f => {
      f(a)
    })
  }

  /** Methods to implement Future **/

  override def isCompleted = successfulVal match {
    case Some(a) => true
    case None => false
  }

  override def onComplete[B](f: Try[A] => B)(implicit executor: ExecutionContext): Unit = {
    this.map((a: A) => f(new Success(a)))
  }

  override def ready(atMost: Duration)(implicit permit: CanAwait): this.type = {
    val p = Promise[A]
    map(a => p.success(a))
    p.future.ready(atMost)
    this
  }

  override def result(atMost: Duration)(implicit permit: CanAwait): A = {
    val p = Promise[A]
    map(a => p.success(a))
    p.future.result(atMost)
  }

  override def value: Option[Try[A]] = successfulVal.map(Try(_))

  override def map[B](mf: A => B)(implicit ec: ExecutionContext): Future[B] = {
    val newl = new ThreadlessPromise[B]()
    successfulVal match {
      case Some(a) =>
        newl.success(mf(a))
        newl.future
      case None =>
        q.add(a => {
          newl.success(mf(a))
        })
        newl.future
    }
  }

  override def flatMap[B](mf: A => Future[B])(implicit ec: ExecutionContext): Future[B] = {
    successfulVal match {
      case Some(a) =>
        mf(a)
      case None =>
        val newl = new ThreadlessPromise[B]()
        q.add(a => {
          val i = mf(a)
          i.map(b => newl.success(b))
        })
        newl.future
    }
  }

  def foreach(f: A => Unit): Unit = {
    q.add(f)
  }
}

object ThreadlessFuture {
  def apply[A](a: A): Future[A] = {
    val l = new ThreadlessPromise[A]()
    l.success(a)
    l.future
  }
}

