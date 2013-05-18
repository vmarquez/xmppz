package xmppz
import scala.concurrent.{ ExecutionContext, Future }
import java.net.InetSocketAddress

import ExecutionContext.Implicits.global
import org.scalatest.FunSuite
import java.util.concurrent.TimeUnit
import java.util.concurrent.CountDownLatch
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import netty._
import util.LogMsg

@RunWith(classOf[JUnitRunner])
class NettyPlumberTest extends FunSuite {

  val latch = new CountDownLatch(1)

  var packets = List[Packet]()

  val str2 = """<stream:features> <mechanisms xmlns="urn:ietf:params:xml:ns:xmpp-sasl"><mechanism>X-OAUTH2</mechanism><mechanism>X-GOOGLE-TOKEN</mechanism><mechanism>PLAIN</mechanism></mechanisms></stream:features>"""

  //val str2 = """<message from='northumberland@shakespeare.lit' id='richard2-4.1.247' to='kingrichard@royalty.england.lit'><body>My lord, dispatch; read o'er these articles.</body></message>"""

  val str1 = """<stream:stream xmlns='jabber:client' xmlns:stream='http://etherx.jabber.org/streams' id='c2s_123' from='example.com' version='1.0'>"""

  test("testing bracketDecoder") {
    println("yay")
    val testServer = new TestNettyServer(8081)
    val plumber = NettyConnectionPlumber(new InetSocketAddress(8081))

    Future {
      try {
        println("ok we're running the plumber")
        plumber.run(incoming, error)
      } catch {
        case ex: Exception => ex.printStackTrace()
      }
      println("finished running plumber")
    }

    Thread.sleep(300)

    //plumber.write("test")

    testServer.writeToClient(str2.substring(0, 50))
    testServer.writeToClient(str2.substring(50, str2.length))
    println("done writing to client.........")
    if (!latch.await(20, TimeUnit.SECONDS)) {
      //testServer.shutdown() 
      assert(false)
    }
  }

  def incoming(log: List[LogMsg[Connection]], p: Seq[Packet]): Unit = {
    println("p = " + p)
    packets = packets ++ p
    if (packets.size > 0)
      latch.countDown()
  }

  def error(log: List[LogMsg[Connection]], cerror: ConnectionError) =
    println(cerror.message.toString)
}

