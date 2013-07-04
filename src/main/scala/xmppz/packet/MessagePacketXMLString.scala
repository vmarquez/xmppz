package xmppz.packet

import xmppz._

object MessagePacketXMLString {

  def XMLString(): PartialFunction[Packet, String => String] = {
    case mp: MessagePacket => (children: String) => MessageXMLString(mp, children)
  }

  def MessageXMLString(mp: MessagePacket, children: String): String = {
    mp match {
      case message: Message =>
        val str = new StringBuilder()
        str.append("<message type='" + message.messageType + "' to='" + message.to + "'")
        str.append(message.from.map(f => " from='" + f + "'").getOrElse(""))
        str.append(" xml:lang='en'>") //TODO: add lang as a message property
        str.append(message.subject.map(s => "<subject>" + s + "</subject>").getOrElse(""))
        str.append("<body>" + message.body.getOrElse("") + "</body>")
        str.append("</message>")
        str.toString

      case presence: Presence =>
        val str = new StringBuilder()
        str.append("<presence")
        str.append(presence.presenceType.map(t => " type='" + t + "'").getOrElse(""))
        str.append(presence.to.map(t => " to='" + t + "'").getOrElse(""))
        str.append(presence.from.map(t => " from='" + t + "'").getOrElse(""))
        str.append(">")
        str.append(presence.show.map(s => "<show>" + s + "</show>").getOrElse(""))
        str.append(presence.status.map(s => "<status>" + s + "</status>").getOrElse(""))
        str.append("</presence>")
        str.toString

      case delay: Delay =>
        val str = new StringBuilder
        str.append("<delay")
        str.append(" stamp ='" + delay.stamp + "'")
        str.append(delay.from.map(s => " from='" + s + "'").getOrElse(""))
        str.append(">")
        str.append(delay.body.getOrElse(""))
        str.append("</delay>")
        str.toString
    }
  }

  /*
  implicit def MessageXMLPacket(s: Session): XMLPacket[Message] = new XMLPacket[Message] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(s).getOrElse("")
  }
  implicit def DelayXMLPacket(d: Delay): XMLPacket[Delay] = new XMLPacket[Delay] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(d).getOrElse("")
  }

  implicit def PresenceXMLPacket(p: Presence): XMLPacket[Presence] = new XMLPacket[Presence] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(p).getOrElse("")
  }

  implicit def RosterXMLPacket(r: Roster): XMLPacket[Roster] = new XMLPacket[Roster] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(r).getOrElse("")
  }

  implicit def ItemXMLPacket(i: Item): XMLPacket[Item] = new XMLPacket[Item] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(i).getOrElse("")
  }

  implicit def XPacketXMLPacket(p: XPacket): XMLPacket[XPacket] = new XMLPacket[XPacket] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(p).getOrElse("")
  }

  implicit def CPacketXMLPacket(p: CPacket): XMLPacket[CPacket] = new XMLPacket[CPacket] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(p).getOrElse("")

  }
  implicit def PriorityXMLPacket(p: Priority): XMLPacket[Priority] = new XMLPacket[Priority] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(p).getOrElse("")
  }
  implicit def ShowXMLPacket(p: Show): XMLPacket[Show] = new XMLPacket[Show] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(p).getOrElse("")
  }

  implicit def PhotoXMLPacket(p: Photo): XMLPacket[Photo] = new XMLPacket[Photo] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(p).getOrElse("")
  }

  implicit def DataXMLPacket(p: Data): XMLPacket[Data] = new XMLPacket[Data] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(p).getOrElse("")
  }

  implicit def RecordXMLPacket(p: Record): XMLPacket[Record] = new XMLPacket[Record] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(p).getOrElse("")
  }

  implicit def ComposingXMLPacket(p: Composing): XMLPacket[Composing] = new XMLPacket[Composing] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(p).getOrElse("")
  }
  */

}