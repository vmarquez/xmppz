package xmppz.util

import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.JavaConversions._
import scala.concurrent.CanAwait
import java.util.concurrent.atomic.AtomicReference
import scala.util.{ Try, Success }
import scala.concurrent.duration.Duration
import scala.concurrent.{ ExecutionContext, Future, Promise }
import ExecutionContext.Implicits.global

//the normal promise shoots off the 'fulfillment' part to a different thread, we don't want that
class ThreadlessPromise[A] extends Future[A] {

  private val q: ConcurrentLinkedQueue[A => Unit] = new ConcurrentLinkedQueue[A => Unit]()

  def future: Future[A] = this

  //private var successfulVal: Option[A] = None
  private val atomicVal = new AtomicReference[Option[A]](None)

  def success(a: A): Unit = {
    if (atomicVal.compareAndSet(None, Some(a)))
      q.foreach(f => {
        f(a)
      })
  }

  /** Methods to implement Future **/

  override def isCompleted = atomicVal.get match {
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

  override def value: Option[Try[A]] = atomicVal.get.map(Try(_))

  override def map[B](mf: A => B)(implicit ec: ExecutionContext): Future[B] = {
    val newp = new ThreadlessPromise[B]()
    q.add(a => {
      newp.success(mf(a))
    })
    atomicVal.get.map(a => newp.success(mf(a)))
    newp
  }

  override def flatMap[B](mf: A => Future[B])(implicit ec: ExecutionContext): Future[B] = {
    //add to the queue first, so we don't have a race. if
    val newp = new ThreadlessPromise[B]()
    q.add(a => {
      val i = mf(a)
      i.foreach(b => newp.success(b))
    })
    atomicVal.get.map(mf(_).foreach(b => newp.success(b)))
    newp.future
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

