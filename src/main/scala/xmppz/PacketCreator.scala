package xmppz

import scala.xml.pull._
import util._
import packet._
trait BaseCreator {

  def getMatchers: List[PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet]]

  def attribute(str: String)(implicit start: EvElemStart) =
    start.attrs.asAttrMap.get(str)
}

object PacketCreator {

  private val unknownParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, name) => (evStart: EvElemStart, children: Seq[Packet]) =>
      println("Name = " + name)
      UnknownPacket(name = name,
        children = children)
  }

  def getBasic() = CorePacketCreator.getMatchers ::: MessagePacketCreator.getMatchers

  def apply(f: Option[Seq[Packet] => Unit] = None,
    pfs: Seq[PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet]] = getBasic()): ElemCreator[Packet] = {

    val aggregateMatch = pfs.foldLeft(PartialFunction.empty[(String, String), (EvElemStart, Seq[Packet]) => Packet])(_ orElse _) orElse unknownParse

    new ElemCreator[Packet] {

      def apply(evStart: EvElemStart, children: Seq[Packet]) =
        aggregateMatch(Option(evStart.pre).getOrElse("").toLowerCase(), Option(evStart.label).getOrElse("").toLowerCase())(evStart, children)

      def toGetChildren(pre: String, label: String) =
        (pre, label) match {
          case (_, "stream") =>
            false
          case _ =>
            true
        }

      def apply(txt: String): Packet = {
        MessageBody(txt)
      }

      def apply(packets: Seq[Packet]) = f.foreach(_(packets))

      def getEnd() = StreamEnd()

      def attribute(str: String)(implicit start: EvElemStart) =
        start.attrs.asAttrMap.get(str)
    }
  }
  case class MessageBody(
    msg: String,
    children: Seq[Packet] = List[Packet]()) extends Packet

  case class Required(
    children: Seq[Packet] = List[Packet]()) extends Packet

  case class StartTLS(
    required: Boolean = false,
    children: Seq[Packet] = List[Packet]()) extends Packet

  case class Mechanisms(
    mechanisms: Seq[Mechanism],
    children: Seq[Packet] = List[Packet]()) extends Packet

  case class Group(
    groupname: String = "",
    children: Seq[Packet] = List[Packet]())
      extends Packet

}

