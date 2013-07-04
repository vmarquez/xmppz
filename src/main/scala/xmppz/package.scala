package xmppz
import scalaz._
import Scalaz._
import scalaz.Monoid
import scala.concurrent.{ ExecutionContext, Future }
import java.util.concurrent.Executors
import scala.collection.IterableLike
import util._
import packet._

package object xmppz {

  implicit val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  implicit val poutput = PacketOutput(List(CorePacketXMLString.XMLString(), MessagePacketXMLString.XMLString()))

  //Typeclass Conversions
  implicit def FutureMonad: Monad[Future] = new Monad[Future] {

    def point[A](a: => A) = scala.concurrent.Future.successful(a) //we should use the non-threaded future here...

    def bind[A, B](f: Future[A])(fmap: A => Future[B]) = f.flatMap(fmap(_))
  }

  implicit def FutureEach: Each[Future] = new Each[Future] {
    def each[A](fa: Future[A])(f: A => Unit) =
      fa.foreach(f)
  }

  implicit def FutureFunctor: Functor[Future] = new Functor[Future] {
    def map[A, B](f: Future[A])(map: A => B): Future[B] = f.map(map(_))
  }

  implicit def ListenerMonoid: Monoid[(Connection, String)] = new Monoid[(Connection, String)] {
    def zero: (Connection, String) = (null, "Error in monoid") ///AAARG SCALAC I HATE THIS THIS IS YOUR FAULT SCALA!!!!

    def append(f1: (Connection, String), f2: => (Connection, String)): (Connection, String) = f1
  }

  //our own types so we don't have to have some  syntax in our method signatures!
  type WriterFuture[+A] = WriterT[Future, List[LogMsg[Connection]], A]

  //right now only called from connection, maybe the wrong place for this, thought we'd use it elsewhre 
  def getTransformer[A, B](future: Future[(List[LogMsg[Connection]], \/[B, A])]): EitherT[WriterFuture, B, A] = {
    val writer = WriterT[Future, List[LogMsg[Connection]], \/[B, A]](future)
    EitherT[WriterFuture, B, A](writer)
  }

  implicit def toWriterHelper[A, B](transformer: EitherT[WriterFuture, (Connection, ConnectionError), Connection]): WriterHelper[Connection] = new WriterHelper[Connection] {

    //this isn't in ScalaZ.WriterT? Should it be added?
    def handleLogs(f: List[LogMsg[Connection]] => Unit): Unit = transformer.run.run.foreach(fw =>
      f(fw._1)
    )
  }

  trait WriterHelper[A] {
    def handleLogs(f: List[LogMsg[Connection]] => Unit): Unit
  }

  //Maybe move this to its own utility class? 
  class SafeTail[A, C <: Iterable[A]](seq: C with IterableLike[A, C]) {
    def safeTail: C =
      seq.isEmpty match {
        case true => seq
        case false => seq.tail
      }
  }

  implicit def toSafeTail[A, C <: Iterable[A]](seq: C with IterableLike[A, C]): SafeTail[A, C] = new SafeTail[A, C](seq)

}
