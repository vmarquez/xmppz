package xmppz.packet

import java.util.UUID
import org.joda.time.{ DateTime, DateTimeZone }
import xmppz._

sealed trait MessagePacket extends Packet

case class Message(
  body: Option[String] = None,
  subject: Option[String] = None,
  thread: Option[String] = None,
  messageType: String = "chat",
  to: String,
  priority: Int = 1,
  from: Option[String] = None,
  id: String = UUID.randomUUID().toString,
  children: Seq[Packet] = List())
    extends MessagePacket

case class Delay( //from xep-0203
  from: Option[String] = None,
  stamp: DateTime = new DateTime(DateTimeZone.UTC),
  body: Option[String] = None,
  children: Seq[Packet] = List()) extends MessagePacket

case class Presence(
  priority: Int = 1,
  show: Option[String] = None,
  status: Option[String] = None,
  presenceType: Option[String] = None,
  id: Option[String] = None,
  to: Option[String] = None,
  from: Option[String] = None,
  children: Seq[Packet] = List())
    extends MessagePacket

case class Roster(
  contacts: List[Item],
  children: Seq[Packet] = List()) extends MessagePacket

case class Item(
  name: String,
  jid: String,
  subscription: String,
  groups: Seq[String],
  children: Seq[Packet] = List())
    extends MessagePacket

case class XPacket(
  xmlns: String,
  children: Seq[Packet] = List())
    extends MessagePacket

case class Composing(
  children: Seq[Packet] = List()) extends MessagePacket

case class Record(
  xmlns: String = "",
  otr: Boolean,
  children: Seq[Packet] = List())
    extends MessagePacket

case class Data(
  mimeType: String,
  string: Data,
  children: Seq[Packet] = List()) extends MessagePacket

case class Photo(
  hash: String,
  children: Seq[Packet] = List()) extends MessagePacket

case class Show(
  msg: String,
  children: Seq[Packet] = List()) extends MessagePacket

case class Status(
  msg: String,
  children: Seq[Packet] = List()) extends MessagePacket

case class Priority(
  value: Int,
  children: Seq[Packet] = List()) extends MessagePacket

case class CPacket(
  children: Seq[Packet] = List())
    extends MessagePacket
