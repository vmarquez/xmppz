package xmppz
import org.scalatest.FunSuite
import scala.xml.pull._
import scala.io.Source

class XMLTestHelper extends FunSuite {

  def getXMLEvents(xml: String): List[XMLEvent] = {
    val reader = new XMLEventReader(Source.fromString(xml))
    var events = List[XMLEvent]()
    while (reader.hasNext)
      events = events :+ reader.next
    events
  }

  def getReader(xml: String) =
    new XMLEventReader(Source.fromString(xml))

}
