package xmppz

import netty._
import util._
import java.io.{ BufferedInputStream, BufferedOutputStream }
import scala.concurrent.ExecutionContext
import scalaz._
import Scalaz._
import xmppz._
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch

object GchatQuickMsg {

  val host = "talk.google.com"
  val port = 5222

  def main(args: Array[String]): Unit = {
    if (args.size < 4)
      println("must pass [username@gmail.com] [password] [destinationJid] [message goes here]")

    val latch = new CountDownLatch(1)

    val tojid = args(2)
    val msgtext = args.splitAt(3)._2.mkString(" ")
    val conn = Connection.create(getConnParams(args(0), args(1)))
   
    // format: OFF
    val result =
      for {
        (conn, myjid)     <- ConnectionHelper.gchatConnect(conn, "xmppzExampleClient")
        (conn, presence)  <- conn.sendGet[Presence](Presence(from=Some(myjid), to=Some(tojid), presenceType=Some("probe")))
        conn              <- conn.send(Message(body=Some(msgtext), to=tojid, from=Some(myjid)))
        conn              <- conn.send(StreamEnd())
        _                 = latch.countDown()
      } yield conn
    // format: ON

    latch.await()
    conn.p.plumber.shutDown()
    //we can check if there's an error here if we want, dig into logs, etc.
    result.handleLogs(logs =>
      logs.foreach { log =>
        println("Thread [" + log.thread + "] Object [" + log.caller.map(c => c.p.authParams.jid + c.hashCode).getOrElse("") + "] msg=[" + log.msg + "]")
      }
    )
  }

  def getConnParams(user: String, pass: String): ConnectionParams = {
    val encoded = SASLHandler.getAuthTextPlain(user, pass, host)
    ConnectionParams(AuthCredentials(user, "gmail.com", host, port, encoded),
      plumber = NettyConnectionPlumber(new InetSocketAddress(host, port)),
      incomingNoMatch = packet => {
        println("  unrequested packets= " + packet.getClass)
        true
      })
  }

}

