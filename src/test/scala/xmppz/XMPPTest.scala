package xmppz
import java.net._
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scalaz.Id
import scalaz.Id._
import scalaz.effect.IO
import scala.xml.pull._
import java.util.concurrent.CountDownLatch
import scala.io.Source
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import java.util.concurrent.TimeUnit
import util._

@RunWith(classOf[JUnitRunner])
class XMPPTest extends XMLTestHelper {

  val messagePacket = Message(source = "", body = Some("hey dude check out this xmpp stuff"), to = "justin@andbit.net", from = Some("vincent.marquez@gmail.com"), subject = Some("xmpp lib"), id = "")

  val streamFeatures = """<stream:features><starttls xmlns="urn:ietf:params:xml:ns:xmpp-tls"><required/></starttls><mechanisms xmlns="urn:ietf:params:xml:ns:xmpp-sasl"><mechanism>X-GOOGLE-TOKEN</mechanism><mechanism>X-OAUTH2</mechanism></mechanisms></stream:features>"""

  val iq = """<iq type="result" id="bind_1"/>"""

  test("start and ending parsing") {
    val parser = XMLParser(PacketCreator(), true)
    val events = getXMLEvents("<stream:stream></stream:stream>")
    parser.run(events.tail) match {
      case Some((a, b)) =>
        println("b = " + b)
      case None =>
        assert(false)
    }
  }

  test("testing stream features parsing") {
    val innerxml = iq
    val foreverLatch = new CountDownLatch(1)
    val endLatch = new CountDownLatch(1)
    val src = new io.Source {
      val iter = ("<stream:stream>" + innerxml).iterator ++
        Iterator.continually { foreverLatch.await(); '\n' }.take(1) ++
        "</stream:stream>".iterator
    }
    val parser = XMLParser(PacketCreator(), true)
    val eventparser = new XMLEventReader(src)
    var events = List[XMLEvent]()
    Future {
      while (eventparser.available) {
        val n = eventparser.next
        events = events :+ n

        parser.run(events) match {
          case None =>
          case Some((a, b)) =>
            events = a
            if (!(b.collect { case iq: IQ => iq }).isEmpty)
              endLatch.countDown()
        }
      }
      println("finished looping, has next")
    }
    if (endLatch.await(5, TimeUnit.SECONDS))
      assert(true)
    else
      assert(false)
  }

  test("testing Packet Collect") {
    val show = Show(msg = "yes")
    val status = Status(msg = "I'm here!", children = List(UnknownPacket(name = "blah"), show))
    val record = Record(otr = false, children = List(UnknownPacket(name = "asdf"), status))

    val s = record.collect({ case s: Show => s })
    assert(true)
  }

  test("stack overflow test") {
    /*
    for (i <- List(1 to 10000) {
        val i = 
        for {
            f1 <- Future { 5 }
            f2 <- Futuer { 6 }
        } yield (f1 + f2)

        i.foreach(println(_))
    }*/
    def innerMethod(f: Future[Int]): Unit = {
      val newp = new ThreadlessPromise[Int]()
      for (fi <- f) {

      }
    }
    val p = new ThreadlessPromise[Int]()

    //get a message, create a future, pass it in
    Future {
      p.success(5)
    }
    readLine()

  }

}

