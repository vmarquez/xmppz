package xmppz.packet

import xmppz._

case class IQ(
  iqType: String,
  id: String,
  isResponse: Boolean = false,
  to: Option[String] = None,
  from: Option[String] = None,
  children: Seq[Packet] = List())
    extends Packet

case class StreamStart(
  id: Option[String] = None,
  domain: String,
  children: Seq[Packet] = List() //always empty, cause of XMPP...
  ) extends Packet

case class StreamEnd(
  children: Seq[Packet] = List())
    extends Packet

case class Error(
  children: Seq[Packet] = List[Packet]())
    extends Packet

case class StreamFeatures(
  starttls: Boolean = false,
  required: Boolean = false,
  mechanisms: Seq[String] = List[String](),
  children: Seq[Packet] = List()) extends Packet

case class Mechanism(
  txt: String,
  children: Seq[Packet] = List()) extends Packet

/*
     * auth related packets
     */

case class Jid(
  value: String = "",
  children: Seq[Packet] = List())
    extends Packet

case class Auth(
  mechanism: String,
  encoded: String,
  children: Seq[Packet] = List())
    extends Packet

case class Challenge(
  challenges: Seq[String],
  children: Seq[Packet] = List())
    extends Packet

case class ChallengeResponse(
  credentials: String,
  children: Seq[Packet] = List()) extends Packet

case class ProceedTLS(
  children: Seq[Packet] = List[Packet]()) extends Packet

case class SASLSuccess(
  children: Seq[Packet] = List[Packet]())
    extends Packet

case class SASLFailure(
  children: Seq[Packet] = List[Packet]()) extends Packet

case class Bind(
  children: Seq[Packet] = List[Packet]()) extends Packet

case class Query(
  xmlns: String,
  children: Seq[Packet] = List[Packet]()) extends Packet

case class StartTLS(
  required: Boolean = false,
  children: Seq[Packet] = List[Packet]()) extends Packet

case class Session(
  xmlns: Option[String] = None,
  children: Seq[Packet] = List[Packet]()) extends Packet

case class UnknownPacket(
  name: String,
  children: Seq[Packet] = List[Packet]()) extends Packet

object CorePackets {

  def bindJid(jid: String): IQ = {
    IQ(iqType = "set",
      id = "bind_0",
      children = List(Bind(children = List[Packet](Jid(value = jid))))
    )
  }

  def bindSession(): IQ = {
    IQ(iqType = "set",
      id = "bind_1",
      children = List(Session()))
  }

  def getRoster(): IQ = {
    IQ(iqType = "get",
      id = "queryRoster2",
      children = List(Query(xmlns = "jabber:iq:roster")))

  }
}

