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
        str.append(children)
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
        str.append(children)
        str.append("</presence>")
        str.toString

      case delay: Delay =>
        val str = new StringBuilder
        str.append("<delay")
        str.append(" stamp ='" + delay.stamp + "'")
        str.append(delay.from.map(s => " from='" + s + "'").getOrElse(""))
        str.append(">")
        str.append(delay.body.getOrElse(""))
        str.append(children)
        str.append("</delay>")
        str.toString
    }
  }

}