package xmppz.packet
import xmppz.Packet

sealed trait PacketOutput {
  def toXMLString(p: Packet): String

}
object PacketOutput {

  def unknown: PartialFunction[Packet, String => String] = { case _ => (str: String) => "" }

  def apply(li: List[PartialFunction[Packet, String => String]]) = new PacketOutput {
    val f = li.foldLeft(PartialFunction.empty[Packet, String => String])(_ orElse _) orElse unknown

    def toXMLString(p: Packet): String = {
      val children: String = p.children.map(toXMLString(_)).foldLeft("")(_ + _)
      f(p)(children)
    }
  }

}
