package xmppz

import java.util.UUID

case class Message(
  source: String = "",
  body: Option[String] = None,
  subject: Option[String] = None,
  thread: Option[String] = None,
  messageType: String = "chat",
  to: String,
  priority: Int = 1,
  from: Option[String] = None,
  id: String = UUID.randomUUID().toString,
  children: Seq[Packet] = List())
    extends Packet

case class Presence(
  source: String = "",
  priority: Int = 1,
  show: Option[String] = None,
  status: Option[String] = None,
  presenceType: Option[String] = None,
  id: Option[String] = None,
  to: Option[String] = None,
  from: Option[String] = None,
  children: Seq[Packet] = List())
    extends Packet

case class Roster(
  source: String = "",
  contacts: List[Item],
  children: Seq[Packet] = List()) extends Packet

case class Item(
  source: String = "",
  name: String,
  jid: String,
  subscription: String,
  groups: Seq[String],
  children: Seq[Packet] = List())
    extends Packet

case class XPacket(
  source: String = "",
  xmlns: String,
  children: Seq[Packet] = List())
    extends Packet

case class Composing(
  source: String = "",
  children: Seq[Packet] = List()) extends Packet

case class Record(
  source: String = "",
  xmlns: String = "",
  otr: Boolean,
  children: Seq[Packet] = List())
    extends Packet

case class Data(
  source: String = "",
  mimeType: String,
  string: Data,
  children: Seq[Packet] = List()) extends Packet

case class Photo(
  source: String = "",
  hash: String,
  children: Seq[Packet] = List()) extends Packet

case class Show(
  source: String = "",
  msg: String,
  children: Seq[Packet] = List()) extends Packet

case class Status(
  source: String = "",
  msg: String,
  children: Seq[Packet] = List()) extends Packet

case class Priority(
  source: String = "",
  value: Int,
  children: Seq[Packet] = List()) extends Packet

case class CPacket(
  source: String = "",
  children: Seq[Packet] = List())
    extends Packet
