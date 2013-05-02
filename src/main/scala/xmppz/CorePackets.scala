package xmppz

import java.util.UUID
case class IQ(
  source: String = "",
  iqType: String,
  id: String,
  isResponse: Boolean = false,
  to: Option[String] = None,
  from: Option[String] = None,
  children: Seq[Packet] = List())
    extends Packet

case class StreamStart(
  source: String = "",
  id: Option[String] = None,
  domain: String,
  children: Seq[Packet] = List() //always empty, cause of XMPP...
  ) extends Packet

case class StreamEnd(source: String = "",
  children: Seq[Packet] = List())
    extends Packet

case class Error(source: String = "",
  children: Seq[Packet] = List[Packet]())
    extends Packet

case class StreamFeatures(
  source: String = "",
  starttls: Boolean = false,
  required: Boolean = false,
  mechanisms: Seq[String] = List[String](),
  children: Seq[Packet] = List()) extends Packet

case class Mechanism(
  source: String = "",
  txt: String,
  children: Seq[Packet] = List()) extends Packet

/*
     * auth related packets
     */

case class Jid(
  source: String = "",
  value: String = "",
  children: Seq[Packet] = List())
    extends Packet

case class Auth(
  source: String = "",
  mechanism: String,
  encoded: String,
  children: Seq[Packet] = List())
    extends Packet

case class Challenge(
  source: String = "",
  challenges: Seq[String],
  children: Seq[Packet] = List())
    extends Packet

case class ChallengeResponse(
  source: String = "",
  credentials: String,
  children: Seq[Packet] = List()) extends Packet

case class ProceedTLS(
  source: String = "",
  children: Seq[Packet] = List[Packet]()) extends Packet

case class SASLSuccess(
  source: String = "",
  children: Seq[Packet] = List[Packet]())
    extends Packet

case class SASLFailure(
  source: String = "",
  children: Seq[Packet] = List[Packet]()) extends Packet

case class Bind(
  source: String = "",
  children: Seq[Packet] = List[Packet]()) extends Packet

case class Query(
  source: String = "",
  xmlns: String,
  children: Seq[Packet] = List[Packet]()) extends Packet

case class StartTLS(
  source: String = "",
  required: Boolean = false,
  children: Seq[Packet] = List[Packet]()) extends Packet

case class Session(
  source: String = "",
  xmlns: Option[String] = None,
  children: Seq[Packet] = List[Packet]()) extends Packet

case class UnknownPacket(
  source: String = "",
  name: String,
  children: Seq[Packet] = List[Packet]()) extends Packet

