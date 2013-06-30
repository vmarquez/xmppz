package xmppz.packet

import scala.xml.pull._
import xmppz._

object CorePacketCreator extends BaseCreator {

  import PacketCreator._

  private val iqparse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "iq") => (evStart: EvElemStart, children: Seq[Packet]) => {
      implicit val ev = evStart
      val roster = children.collect { case q: Query => if (q.xmlns == "jabber:iq:roster") q }
      attribute("type") match {
        case Some("result") if !(roster.isEmpty) =>
          val items = roster.collect { case item: Item => item }
          Roster(contacts = items.toList)
        case _ =>
          IQ(
            id = attribute("id").getOrElse(""),
            iqType = attribute("type").getOrElse(""),
            from = attribute("from"),
            children = children)
      }
    }
  }

  private val streamStartParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case ("stream", "stream") | (_, "stream") => (evStart: EvElemStart, children: Seq[Packet]) =>
      StreamStart(domain = "")
  }

  private val streamFeaturesParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case ("stream", "features") => (evStart: EvElemStart, children: Seq[Packet]) =>
      val tls = children.collect { case startTLS: StartTLS => startTLS }
      val mechanisms = children.collect({ case m: Mechanisms => m })
        .headOption match {
          case Some(m) => m.mechanisms
          case None => Seq[Mechanism]()
        }
      StreamFeatures(
        starttls = if (tls == None) false else true,
        required = true,
        mechanisms = mechanisms.map(m => m.txt))
  }

  private val startTlsParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "starttls") => (evStart: EvElemStart, children: Seq[Packet]) =>
      //println("START TLS")
      val required = children.collect { case req: Required => true }
        .headOption
        .getOrElse(false)
      StartTLS(required = required)
  }

  private val proceedParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "proceed") => (evStart: EvElemStart, children: Seq[Packet]) =>
      ProceedTLS()
  }

  private val successParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "success") => (evStart: EvElemStart, children: Seq[Packet]) =>
      SASLSuccess()
  }

  private val failureParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "failure") => (evStart: EvElemStart, children: Seq[Packet]) =>
      SASLFailure()
  }

  private val requiredParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "required") => (evStart: EvElemStart, children: Seq[Packet]) =>
      Required()
  }

  private val sessionParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] =
    {
      case (_, "session") => (evStart: EvElemStart, children: Seq[Packet]) =>
        implicit val ev = evStart
        Session(xmlns = attribute("xmlns"))
    }

  private val mechanismParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] =
    {
      case (_, "mechanism") => (evStart: EvElemStart, children: Seq[Packet]) =>
        val mechanismtxt = children.collect { case m: MessageBody => m.msg }
          .headOption
          .getOrElse("")
        Mechanism(txt = mechanismtxt)
    }
  private val mechanismsParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "mechanisms") => (evStart: EvElemStart, children: Seq[Packet]) =>
      val mechanisms = children.collect { case m: Mechanism => m }
      Mechanisms(mechanisms = mechanisms)
  }

  private val bindParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] =
    {
      case (_, "bind") => (evStart: EvElemStart, children: Seq[Packet]) =>
        Bind(children = children)
    }

  private val jidParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] =
    {
      case (_, "jid") => (evStart: EvElemStart, children: Seq[Packet]) =>
        val txt = children.collect({
          case m: MessageBody => {
            m.msg
          }
        })
          .headOption
        println(" Teeeeext = " + txt)
        Jid(value = txt.getOrElse(""))
    }

  private val queryParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "query") => (evStart: EvElemStart, children: Seq[Packet]) =>
      implicit val ev = evStart
      Query(xmlns = attribute("xmlns").getOrElse(""), children = children)
  }

  private val groupParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "group") => (evStart: EvElemStart, children: Seq[Packet]) =>
      val txt = children
        .collect({ case m: MessageBody => m.msg })
        .headOption
        .getOrElse("")
      Group(groupname = txt)
  }

  def getMatchers =
    List[PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet]](
      iqparse,
      streamStartParse,
      streamFeaturesParse,
      startTlsParse,
      proceedParse,
      successParse,
      failureParse,
      requiredParse,
      sessionParse,
      mechanismParse,
      mechanismsParse,
      bindParse,
      jidParse,
      queryParse,
      groupParse
    )
}
