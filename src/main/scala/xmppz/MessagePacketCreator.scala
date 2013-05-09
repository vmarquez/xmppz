package xmppz

import scala.xml.pull._
//import util._

object MessagePacketCreator extends CreatorHelper {
  def getMatchers =
    List[PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet]](
      messageParse,
      presenceParse,
      statusParse,
      groupParse,
      itemParse,
      xParse,
      composingParse,
      recordParse,
      photoParse,
      showParse,
      cParse,
      priorityParse

    )

  private val messageParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "message") => (evStart: EvElemStart, children: Seq[Packet]) => {
      implicit val ev = evStart
      val txt = children.collect({ case m: MessageBody => m.msg })
      Message(
        source = evStart.toString,
        body = txt.headOption,
        subject = attribute("subject"),
        thread = attribute("thread"),
        messageType = attribute("type").getOrElse(""),
        to = attribute("to").getOrElse(""), //should we ignore the whole message? probably... 
        from = attribute("from"),
        id = attribute("id").getOrElse(""))

    }
  }

  private val presenceParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "presence") => (evStart: EvElemStart, children: Seq[Packet]) => {
      implicit val ev = evStart
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
    }
  }

  private val statusParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "status") => (evStart: EvElemStart, children: Seq[Packet]) => {
      val txt = children.collect { case m: MessageBody => m.msg }.headOption.getOrElse("")
      Status(source = evStart.toString,
        msg = txt)
    }
  }

  private val groupParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "group") => (evStart: EvElemStart, children: Seq[Packet]) => {
      val txt = children
        .collect({ case m: MessageBody => m.msg })
        .headOption
        .getOrElse("")
      Group(source = evStart.toString, groupname = txt)
    }
  }

  private val itemParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "item") => (evStart: EvElemStart, children: Seq[Packet]) => {
      implicit val ev = evStart
      val grps = children.collect { case g: Group => g.groupname }
      Item(
        source = evStart.toString,
        name = attribute("name").getOrElse(""),
        jid = attribute("jid").getOrElse(""),
        subscription = attribute("subscription").getOrElse(""),
        groups = grps)
    }
  }
  private val xParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "x") => (evStart: EvElemStart, children: Seq[Packet]) => {
      implicit val ev = evStart
      XPacket(source = evStart.toString,
        xmlns = attribute("xmlns").getOrElse(""),
        children = children)
    }
  }

  private val composingParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "composing") => (evStart: EvElemStart, children: Seq[Packet]) =>
      Composing(source = evStart.toString)
  }

  private val recordParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "record") => (evStart: EvElemStart, children: Seq[Packet]) => {
      implicit val ev = evStart
      Record(source = evStart.toString,
        xmlns = attribute("xmlns").getOrElse(""),
        otr = (attribute("") == "true"))
    }
  }

  private val showParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "show") => (evStart: EvElemStart, children: Seq[Packet]) => {
      val txt = children.collect { case m: MessageBody => m.msg }.headOption.getOrElse("")
      Show(source = evStart.toString,
        msg = txt)
    }
  }

  private val photoParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "photo") => (evStart: EvElemStart, children: Seq[Packet]) => {
      val photohash = children.collect { case m: MessageBody => m.msg }.headOption.getOrElse("")
      Photo(source = evStart.toString,
        hash = photohash)
    }
  }
  private val cParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "c") => (evStart: EvElemStart, children: Seq[Packet]) =>
      CPacket(source = evStart.toString)
  }

  private val priorityParse: PartialFunction[(String, String), (EvElemStart, Seq[Packet]) => Packet] = {
    case (_, "priority") => (evStart: EvElemStart, children: Seq[Packet]) =>
      Priority(source = evStart.toString,
        value = Integer.parseInt(children.collect { case m: MessageBody => m.msg }.headOption.getOrElse("")))
  }
}

