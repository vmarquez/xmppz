package xmppz

import scala.xml.pull._
import util._

/*
TODO: redesign to make this modular.  Should have each packet PF's in a separate object for each RFC, along with their case statements
finally, we should construct a final PacketCreator out of a collection of case statements from each RFC we want to be able to parse:

*/
object PacketCreator {
  //<iq id="bind_0" type="result"><bind xmlns="urn:ietf:params:xml:ns:xmpp-bind"><jid>D4Rkph1b3r@gmail.com/xmppzED1FE4A9</jid></bind></iq> 
  def apply(f: Option[Seq[Packet] => Unit] = None) = new ElemCreator[Packet] {
    //FIXME: should return an EITHER, what if they send some packet type we don't know about? that should signal that we should log even though we're able to proceed 
    def apply(evStart: EvElemStart, children: Seq[Packet]): Packet = {
      implicit val ev = evStart
      val ret =
        (Option(evStart.pre).getOrElse("").toLowerCase(), Option(evStart.label).getOrElse("").toLowerCase()) match {

          case (_, "iq") =>
            val roster = children.collect { case q: Query => if (q.xmlns == "jabber:iq:roster") q }
            //                    val jid = children.collect{ 
            //check if it's a roster packet
            attribute("type") match {
              case Some("result") if !(roster.isEmpty) =>
                val items = roster.collect { case item: Item => item }
                Roster(source = evStart.toString, contacts = items.toList)

              case _ =>
                IQ(source = evStart.toString,
                  id = attribute("id").getOrElse(""),
                  iqType = attribute("type").getOrElse(""),
                  from = attribute("from"),
                  children = children)
            }
          case (_, "message") =>
            val txt = children.collect({ case m: MessageBody => m.msg })
            Message(
              body = txt.headOption,
              source = evStart.toString,
              subject = attribute("subject"),
              thread = attribute("thread"),
              messageType = attribute("type").getOrElse(""),
              to = attribute("to").getOrElse(""), //should we ignore the whole message? probably... 
              from = attribute("from"),
              id = attribute("id").getOrElse(""))

          case (_, "presence") =>
            val status = children.collect({ case s: Status => s.msg }).headOption
            val show = children.collect({ case s: Show => s.msg }).headOption
            val priority = children.collect({ case p: Priority => p.value }).headOption
            Presence(
              source = evStart.toString,
              priority = 0,
              show = show,
              status = status,
              presenceType = attribute("type"),
              id = attribute("id"),
              from = attribute("from"),
              to = attribute("to"),
              children = children)

          case ("stream", "stream") | (_, "stream") =>
            StreamStart(source = evStart.toString, domain = "")

          case ("stream", "features") =>
            val tls = children.collect { case startTLS: StartTLS => startTLS }
            val mechanisms = children.collect({ case m: Mechanisms => m })
              .headOption match {
                case Some(m) => m.mechanisms
                case None => Seq[Mechanism]()
              }
            StreamFeatures(source = evStart.toString,
              starttls = if (tls == None) false else true,
              required = true,
              mechanisms = mechanisms.map(m => m.txt))

          case (_, "starttls") =>
            //println("START TLS")
            val required = children.collect { case req: Required => true }
              .headOption
              .getOrElse(false)
            StartTLS(source = evStart.toString, required = required)

          case (_, "proceed") =>
            ProceedTLS(source = evStart.toString)

          case (_, "success") =>
            SASLSuccess(source = evStart.toString)

          case (_, "failure") =>
            SASLFailure(source = evStart.toString)

          case (_, "body") =>
            children.collect({ case m: MessageBody => m })
              .headOption
              .getOrElse(MessageBody("", ""))

          case (_, "required") =>
            Required(source = evStart.toString)

          case (_, "session") =>
            Session(source = evStart.toString, xmlns = attribute("xmlns"))

          case (_, "mechanism") =>
            val mechanismtxt = children.collect { case m: MessageBody => m.msg }
              .headOption
              .getOrElse("")
            Mechanism(source = evStart.toString, txt = mechanismtxt)

          case (_, "mechanisms") =>
            val mechanisms = children.collect { case m: Mechanism => m }
            Mechanisms(source = evStart.toString, mechanisms = mechanisms)

          case (_, "bind") =>
            Bind(source = evStart.toString, children = children)

          case (_, "jid") =>
            val txt = children.collect({ case m: MessageBody => m.msg })
              .headOption
            Jid(source = evStart.toString, value = txt.getOrElse(""))

          case (_, "query") =>
            Query(source = evStart.toString, xmlns = attribute("xmlns").getOrElse(""), children = children)

          case (_, "group") =>
            val txt = children
              .collect({ case m: MessageBody => m.msg })
              .headOption
              .getOrElse("")
            Group(source = evStart.toString, groupname = txt)

          case (_, "item") =>
            val grps = children.collect { case g: Group => g.groupname }
            Item(
              source = evStart.toString,
              name = attribute("name").getOrElse(""),
              jid = attribute("jid").getOrElse(""),
              subscription = attribute("subscription").getOrElse(""),
              groups = grps)

          case (_, "x") =>
            XPacket(source = evStart.toString,
              xmlns = attribute("xmlns").getOrElse(""),
              children = children)

          case (_, "composing") =>
            Composing(source = evStart.toString)

          case (_, "record") =>
            Record(source = evStart.toString,
              xmlns = attribute("xmlns").getOrElse(""),
              otr = (attribute("") == "true"))

          case (_, "status") =>
            val txt = children.collect { case m: MessageBody => m.msg }.headOption.getOrElse("")
            Status(source = evStart.toString,
              msg = txt)

          case (_, "show") =>
            val txt = children.collect { case m: MessageBody => m.msg }.headOption.getOrElse("")
            Show(source = evStart.toString,
              msg = txt)

          case (_, "photo") =>
            val photohash = children.collect { case m: MessageBody => m.msg }.headOption.getOrElse("")
            Photo(source = evStart.toString,
              hash = photohash)

          case (_, "c") =>
            CPacket(source = evStart.toString)

          case (_, "priority") =>
            Priority(source = evStart.toString,
              value = Integer.parseInt(children.collect { case m: MessageBody => m.msg }.headOption.getOrElse("")))

          case (_, name) =>
            println(" WE GOT SOMETHING WEIRD = " + name)
            UnknownPacket(source = evStart.toString,
              name = name,
              children = children)
          //throw new Exception("ERORR")
        }
      ret
    }

    def toGetChildren(pre: String, label: String) =
      (pre, label) match {
        case (_, "stream") =>
          false
        case _ =>
          true
      }

    def apply(txt: String): Packet = {
      MessageBody(txt, txt)
    }

    def apply(packets: Seq[Packet]) = f.foreach(_(packets))

    def getEnd() = StreamEnd()

    private def attribute(str: String)(implicit start: EvElemStart) =
      start.attrs.asAttrMap.get(str)
  }

  protected[xmppz] case class MessageBody(
    source: String,
    msg: String,
    children: Seq[Packet] = List[Packet]()) extends Packet

  protected[xmppz] case class Required(
    source: String,
    children: Seq[Packet] = List[Packet]()) extends Packet

  case class StartTLS(
    source: String = "",
    required: Boolean = false,
    children: Seq[Packet] = List[Packet]()) extends Packet

  case class Mechanisms(
    source: String = "",
    mechanisms: Seq[Mechanism],
    children: Seq[Packet] = List[Packet]()) extends Packet

  case class Group(
    source: String = "",
    groupname: String = "",
    children: Seq[Packet] = List[Packet]())
      extends Packet
}

