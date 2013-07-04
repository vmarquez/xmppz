package xmppz.netty

import scalaz._
import Scalaz._
import org.jboss.netty.channel.{ Channel, ChannelHandlerContext }
import org.jboss.netty.buffer.ChannelBuffer
import java.io.{ PipedInputStream, PipedOutputStream }
import java.lang.StringBuilder
import scala.io.Source
import scala.xml.pull.XMLEventReader
import scala.xml.pull.XMLEvent
import xmppz._
import util._
import org.jboss.netty.handler.codec.frame.FrameDecoder
import PacketCreator.MessageBody
import util.LogMsg
import packet.PacketReader

//TODO: should we return an Either[Seq[T],Exception], along iwth Logs!
//should be an XMLDecoder and take a factory for generating XML
class EventDecoder[T](elemCreator: ElemCreator[T]) extends FrameDecoder {

  private val outputStream = new PipedOutputStream
  private val inputStream = new PipedInputStream(outputStream)
  private val xmlEventListener = new XMLEventReader(Source.fromInputStream(inputStream))
  private var xmlevents = List[XMLEvent]()
  private val parser = XMLParser(elemCreator, true)
  private var run = true

  def shutdown(): Unit = {
    inputStream.close()
    outputStream.close()
  }

  //TODO: MAKE FUNCTIONAL
  //returns an XMLEvent  TODO: LoGGING HERE!
  //So we have some interesting options here. we could return an eitehrT[Writer, or just a \/ of tuples...
  override def decode(ctx: ChannelHandlerContext, channel: Channel, buf: ChannelBuffer): \/[(List[LogMsg[Connection]], ConnectionError), (List[LogMsg[Connection]], Seq[T])] = {
    var packets = List[T]()
    var logs = List[LogMsg[Connection]]()
    var incomingData = ""
    try {
      val readablebytes = buf.readableBytes()
      buf.markReaderIndex()
      val arr = buf.readBytes(readablebytes).array()
      outputStream.write(arr)
      outputStream.flush()
      run = true

      incomingData = new String(arr)
      println("incoming data =" + incomingData)
      //TODO: send up a list of List[LogMsg]
      logs = logs :+ Log[Connection]("TRACE", incomingData)
      val t = PacketReader(xmlevents, xmlEventListener, parser)
      xmlevents = t._1
      packets = packets ::: t._2

    } catch {
      case ex: Exception =>
        val fmt = formatStr(packets)

        println("exception = " + ex.toString)
        ex.printStackTrace
        return (logs, ConnectionError(exception = Some(ex), message = "Data = " + incomingData + "\\n" + fmt)).left
    }
    //We get sent message bodies for KEEP ALIVE, but we only care if they're inside another packet. otherwise useless whtepsace so we filter them
    packets = packets.filter {
      case m: MessageBody =>
        false
      case _ =>
        true
    }
    (logs, packets).right //butwhat if we get nothing? should we call .left?
  }

  def formatStr(packets: List[T]): String = {
    val blder = new StringBuilder()
    blder.append(" \n events = ")
    for (ev <- xmlevents)
      blder.append(ev.toString)

    blder.append(" \n ")
    blder.append(" packets =")
    for (p <- packets)
      blder.append(p + " ")

    blder.toString
  }
}

