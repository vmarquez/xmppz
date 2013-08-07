package xmppz.packet

import xmppz.{ Packet, XMLPacket }

object CorePacketXMLString {

  def XMLString(): PartialFunction[Packet, String => String] = {
    case cp: CorePacket => (children: String) => CoreXMLString(cp, children)
  }

  def CoreXMLString(cp: CorePacket, children: String): String = {
    cp match {
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
        str.append(children)
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
    }
  }

}

