package xmppz.packet

import xmppz._

sealed trait CorePacket extends Packet

object CorePacket {
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

case class IQ(
  iqType: String,
  id: String,
  isResponse: Boolean = false,
  to: Option[String] = None,
  from: Option[String] = None,
  children: Seq[Packet] = List())
    extends CorePacket

case class StreamStart(
  id: Option[String] = None,
  domain: String,
  children: Seq[Packet] = List() //always empty, cause of XMPP...
  ) extends CorePacket

case class StreamEnd(
  children: Seq[Packet] = List())
    extends CorePacket

case class Error(
  children: Seq[Packet] = List[Packet]())
    extends CorePacket

case class StreamFeatures(
  starttls: Boolean = false,
  required: Boolean = false,
  mechanisms: Seq[String] = List[String](),
  children: Seq[Packet] = List()) extends CorePacket

case class Mechanism(
  txt: String,
  children: Seq[Packet] = List()) extends CorePacket

/*
* auth related packets
*/

case class Jid(
  value: String = "",
  children: Seq[Packet] = List())
    extends CorePacket

case class Auth(
  mechanism: String,
  encoded: String,
  children: Seq[Packet] = List())
    extends CorePacket

case class Challenge(
  challenges: Seq[String],
  children: Seq[Packet] = List())
    extends CorePacket

case class ChallengeResponse(
  credentials: String,
  children: Seq[Packet] = List()) extends CorePacket

case class ProceedTLS(
  children: Seq[Packet] = List[Packet]()) extends CorePacket

case class SASLSuccess(
  children: Seq[Packet] = List[Packet]())
    extends CorePacket

case class SASLFailure(
  children: Seq[Packet] = List[Packet]()) extends CorePacket

case class Bind(
  children: Seq[Packet] = List[Packet]()) extends CorePacket

case class Query(
  xmlns: String,
  children: Seq[Packet] = List[Packet]()) extends CorePacket

case class StartTLS(
  required: Boolean = false,
  children: Seq[Packet] = List[Packet]()) extends CorePacket

case class Session(
  xmlns: Option[String] = None,
  children: Seq[Packet] = List[Packet]()) extends CorePacket

case class UnknownPacket(
  name: String,
  children: Seq[Packet] = List[Packet]()) extends CorePacket

