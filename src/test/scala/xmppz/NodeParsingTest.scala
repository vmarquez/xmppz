package xmppz
import util._
import util.XMLParser._

import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import scala.xml._
import scala.xml.pull._
import util._

@RunWith(classOf[JUnitRunner])
class NodeParsingTest extends XMLTestHelper {

  val str1 = <tag attr='somestuff'><b><sub:subtag><mostnested><b>SomeText</b></mostnested></sub:subtag><secondsubtag attr='blah'></secondsubtag><thirdsubtab></thirdsubtab></b></tag>
  //val str2 = <tag attr='somestuff'><b><sub:subtag><mostnested><b></b></mostnested></sub:subtag><secondsubtag attr='blah'></secondsubtag><thirdsubtab></thirdsubtab></b></tag>
  val str3 = <a><b><c></c></b><d></d></a>
  val str4 = <a><b></b></a>
  val str5 = "<a> <c somestuff='woot'/> </a>"

  test("Test successful XML Parsing") {
    val events = getXMLEvents(str1.toString)

    val results = XMLParser[Node](XMLNodeCreator(), true).apply(events)
    //println("RESULTS == " + results)
    results match {
      case None => assert(false)
      case Some(n) => assert(n._2.headOption.get.asInstanceOf[Elem] == str1)
    }
  }

  test("Test incomplete XML parsing") {
    val events = getXMLEvents(str1.toString)
    val partialevents = events.reverse.tail.tail.tail.reverse
    println("partialevents = " + partialevents)
    val results = XMLParser[Node](XMLNodeCreator()).apply(partialevents)

    results match {
      case None => assert(true)
      case Some(r) => assert(false)
    }
  }

  test("Test Complete/notcomplete parsing") {
    val events = getXMLEvents(str4.toString) ++ getXMLEvents(str5.toString)
    val missingEndEvent = events.reverse.tail.reverse
    val results = XMLParser[Node](XMLNodeCreator(), true).apply(missingEndEvent)
    results match {
      case Some((ev, nodes)) if (ev.size > 0) => assert(true)
      case _ => assert(false)

    }
  }

  test("SimpleTest") {
    val events = getXMLEvents(str5.toString)
    println("EVEnts = " + events)
    println("going to try to parse the events = " + events)

    //val results = XMLParser[Packet](PacketCreator(),true).apply(events)
    val results = XMLParser[Node](XMLNodeCreator(), true).apply(events)
    println("OK HERE results  = " + results)
    results match {
      case Some((ev, nodes)) if (nodes.size > 0) =>
        println("yay we got an iq packet")
        assert(true)
      case _ =>
        println("wtf")
        assert(false)
    }
  }

}
