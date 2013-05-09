package xmppz

trait Packet {
  def source: String
  def children: Seq[Packet]

  //TODO: make tail recursive?
  def collect[T <: Packet](pf: PartialFunction[Packet, T]): Option[T] = {
    val first = children.collect(pf).headOption
    first match {
      case Some(f) => Some(f)
      case None => children
        .toSeq
        .map(child => child.collect[T](pf))
        .collect { case Some(x) => x }
        .headOption
    }
  }
}

object Packet {

  implicit def toPacketStr[A <: Packet](p: A) = new PacketStr[A](p)

  class PacketStr[A <: Packet](p: A) {
    def toXMLString: String =
      Packet.toXmlString(p)
  }

  def getChildren(children: Seq[Packet]) =
    children.map(toXmlString(_)).mkString("")

  val emptystr = ""

  def toXmlString(packet: Packet): String = {
    val str =
      packet match {
        case iq: IQ =>
          val str = new StringBuilder()
          ("<iq id='" + iq.id + "' type='" + iq.iqType + "'>" + getChildren(iq.children) + "</iq>").toString
        case message: Message =>
          (<message to={ message.to } from={ message.from.getOrElse("") } subject={ message.subject.getOrElse(emptystr) } type={ message.messageType } xml:lang='en'><body>{ message.body.getOrElse(emptystr) }</body></message>).toString
        //	  	  	(<message to={message.to} from={message.from.getOrElse(emptystr)} subject={message.subject.getOrElse(emptystr)} type={message.messageType} xml:lang='en'><body>{message.body.getOrElse(emptystr)}</body></message>).toString
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

        case streamFeature: StreamFeatures =>
          val str = new StringBuilder()
          str.append("<stream:features>")
          if (streamFeature.starttls)
            str.append("<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'>")
          if (streamFeature.required)
            str.append(" <required/>")
          str.append("<mechanisms xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>")
          for (mechanism <- streamFeature.mechanisms)
            str.append("<mechanism>" + mechanism + "</mechanism>")
          str.append("</mechanisms>")
          str.append("</stream:feature>").toString
        case session: Session =>
          (<session xmlns="urn:ietf:params:xml:ns:xmpp-session"/>).toString
        case clientStartTLS: StartTLS =>
          (<starttls xmlns='urn:ietf:params:xml:ns:xmpp-tls'/>).toString
        case streamStart: StreamStart =>
          "<stream:stream xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' to='" + streamStart.domain + "' version='1.0' xml:lang='en' xmlns:xml='http://www.w3.org/XML/1998/namespace'>"
        case clientAuth: Auth =>
          (<auth xmlns='urn:ietf:params:xml:ns:xmpp-sasl' mechanism={ clientAuth.mechanism }>{ clientAuth.encoded }</auth>).toString
        case challengeResponse: ChallengeResponse =>
          "<response xmlns='urn:ietf:params:xml:ns:xmpp-sasl'>" + challengeResponse.credentials + "</response>"
        case bind: Bind =>
          val str = new StringBuilder()
          str.append("<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>")
          bind.collect { case j: Jid => j }
            .foreach(jid => str.append("<resource>xmppz</resource>"))
          str.append("</bind>").toString
        //(<bind xmlns='urn:ietf:params:xml:ns:xmpp-bind'>{bind.jid.map(jid => <jid>{jid}</jid>)}</bind>).toString
        case query: Query =>
          (<query xmlns={ query.xmlns }/>).toString
        case streamEnd: StreamEnd =>
          "</stream:stream>"
        case _ =>
          println("well oh fuck")
          ""
      }
    str
  }
}
