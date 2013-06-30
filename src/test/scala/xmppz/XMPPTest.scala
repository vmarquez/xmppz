package xmppz

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.joda.time.DateTime
import scala.xml.pull._
import java.util.concurrent.{ Executors, CountDownLatch, TimeUnit }
import scala.concurrent.{ ExecutionContext, Future }
import util._
import packet._

@RunWith(classOf[JUnitRunner])
class XMPPTest extends XMLTestHelper {

  val messagePacket = Message(body = Some("hey dude check out this xmpp stuff"),
    to = "justin@andbit.net", from = Some("vincent.marquez@gmail.com"),
    subject = Some("xmpp lib"),
    children = List[Packet](Delay(stamp = DateTime.parse("2002-09-10T23:08:25Z"), from = Some("vincent.marquez@gmail.com"))),
    id = "")

  val streamFeatures = """<stream:features><starttls xmlns="urn:ietf:params:xml:ns:xmpp-tls"><required/></starttls><mechanisms xmlns="urn:ietf:params:xml:ns:xmpp-sasl"><mechanism>X-GOOGLE-TOKEN</mechanism><mechanism>X-OAUTH2</mechanism></mechanisms></stream:features>"""

  val iq = """<iq type="result" id="bind_1"/>"""

  test("testing message parseing") {
    val s = Packet.toXmlString(messagePacket)
    if (parseString(s, (b: List[Packet]) => {
      b.collect({ case m: Message => m }).headOption.flatMap(o => o.collect({ case d: Delay => d })).map(d => true).getOrElse(false)
    }
    ).await(5, TimeUnit.SECONDS))
      assert(true)
    else
      assert(false)
  }

  test("start and ending parsing") {
    val parser = XMLParser(PacketCreator(), true)
    val events = getXMLEvents("<stream:stream></stream:stream>")
    parser.run(events.tail) match {
      case Some((a, b)) =>
      case None =>
        assert(false)
    }
  }

  test("testing iq parsing") {
    if (parseString(iq, (b: List[Packet]) => !(b.collect { case iq: IQ => iq }.isEmpty)).await(5, TimeUnit.SECONDS))
      assert(true)
    else
      assert(false)
  }

  def parseString(str: String, f: List[Packet] => Boolean): CountDownLatch = {
    implicit var ec: ExecutionContext = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool)
    val endLatch = new CountDownLatch(1)
    val foreverLatch = new CountDownLatch(1)
    val src = new io.Source {
      val iter = ("<stream:stream>" + str).iterator ++
        Iterator.continually { foreverLatch.await(); '\n' }.take(1) ++
        "</stream:stream>".iterator
    }
    val eventparser = new XMLEventReader(src)
    val events = List[XMLEvent]()
    Future {
      val ret = PacketReader(events, eventparser, XMLParser(PacketCreator(), true))
      if (f(ret._2)) {
        endLatch.countDown()
      }
    }
    endLatch
  }

}

