package xmppz.packet

import xmppz.{ Packet, XMLPacket }

object CorePacketXMLString {
  /*
  implicit def IqXMLPacket(iq: IQ): XMLPacket[IQ] = new XMLPacket[IQ] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(iq).getOrElse("")
  }

  implicit def StreamStartXMLPacket(s: StreamStart): XMLPacket[StreamStart] = new XMLPacket[StreamStart] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(s).getOrElse("")
  }

  implicit def StreamEndXMLPacket(s: StreamEnd): XMLPacket[StreamEnd] = new XMLPacket[StreamEnd] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(s).getOrElse("")
  }

  implicit def ErrorXMLPacket(e: Error): XMLPacket[Error] = new XMLPacket[Error] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(e).getOrElse("")
  }

  implicit def StreamFeaturesXMLPacket(s: StreamFeatures): XMLPacket[StreamFeatures] = new XMLPacket[StreamFeatures] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(s).getOrElse("")
  }

  implicit def MechanismXMLPacket(m: Mechanism): XMLPacket[Mechanism] = new XMLPacket[Mechanism] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(m).getOrElse("")
  }

  implicit def JidXMLPacket(j: Jid): XMLPacket[Jid] = new XMLPacket[Jid] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(j).getOrElse("")
  }

  implicit def AuthXMLPacket(a: Auth): XMLPacket[Auth] = new XMLPacket[Auth] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(a).getOrElse("")
  }

  implicit def ChallengeXMLPacket(c: Challenge): XMLPacket[Challenge] = new XMLPacket[Challenge] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(c).getOrElse("")
  }

  implicit def ChallengeResponseXMLPacket(c: ChallengeResponse): XMLPacket[Jid] = new XMLPacket[Jid] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(c).getOrElse("")
  }

  implicit def ProceedTLSXMLPacket(p: ProceedTLS): XMLPacket[ProceedTLS] = new XMLPacket[ProceedTLS] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(p).getOrElse("")
  }

  implicit def SASLSuccessXMLPacket(s: SASLSuccess): XMLPacket[SASLSuccess] = new XMLPacket[SASLSuccess] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(s).getOrElse("")
  }

  implicit def SASLFailureXMLPacket(s: SASLFailure): XMLPacket[SASLFailure] = new XMLPacket[SASLFailure] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(s).getOrElse("")
  }

  implicit def BindXMLPacket(b: Bind): XMLPacket[Bind] = new XMLPacket[Bind] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(b).getOrElse("")
  }

  implicit def QueryXMLPacket(q: Query): XMLPacket[Query] = new XMLPacket[Query] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(q).getOrElse("")
  }

  implicit def SessionXMLPacket(s: Session): XMLPacket[Session] = new XMLPacket[Session] {
    def toXMLString(implicit f: Packet => String): String = XMLString(f).lift(s).getOrElse("")
  }
  */

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

