package xmppz.util

import scalaz._
import Scalaz._
import scalaz.StateT
import scalaz.State
import scala.xml.Node
import scala.xml.pull._
import scala.xml.Elem
import scala.xml._
import scalaz.{ Id, \/ }
import scalaz.\/._
import scalaz.EitherT
import scala.collection.immutable.Stack
import xmppz._

//TODO parser logging, handle end tags? 
object XMLParser {

  def blankState[T] = StateT[Option, List[XMLEvent], List[T]](list => Some((list, List[T]())))
    // format: OFF
  def apply[T](creator: ElemCreator[T], root: Boolean = false): StateT[Option, List[XMLEvent], List[T]] = StateT[Option, List[XMLEvent], List[T]](list => {
    list.headOption match {
      case None =>
        None

      case Some(EvElemEnd(_, _)) if (root)=>
        Some(list.tail, List[T](creator.getEnd))

      case Some(EvElemEnd(_, _)) =>
        Some(list.tail, List[T]())

      case Some(EvText(txt)) =>
        (for {
          nodes <- XMLParser(creator, root)
        } yield creator(txt) :: nodes)
          .apply(list.tail)

      case Some(ev: EvElemStart) if (creator.toGetChildren(ev.pre, ev.label)) =>
        val s =
          (for {
            children    <- XMLParser(creator)
            nodes       <- if (root)
                             blankState
                           else
                             XMLParser(creator)
          } yield (creator(ev, children) :: nodes))
            .apply(list.tail)
        s

      case Some(ev: EvElemStart) =>
        Some(list.tail, List[T](creator(ev, List[T]()))) //for never ending streams like XMPP where everything starts with <stream>...

      case _ =>
        XMLParser(creator)(list.tail)
    }
  })
  // format: ON
}

object XMLNodeCreator {
  def apply(f: Option[Seq[Node] => Unit] = None) = new ElemCreator[Node] {
    def apply(start: EvElemStart, children: Seq[Node]) =
      new Elem(start.pre, start.label, start.attrs, start.scope, children: _*)

    def apply(txt: String): Node =
      new Text(txt)

    def toGetChildren(pre: String, label: String) = true

    def apply(nodes: Seq[Node]) = f.foreach(_(nodes))

    def getEnd() = throw new UnsupportedOperationException
  }
}

trait ElemCreator[T] {
  def apply(start: EvElemStart, children: Seq[T]): T
  def apply(txt: String): T
  def toGetChildren(pre: String, label: String): Boolean
  def apply(t: Seq[T]): Unit
  def getEnd(): T
}
