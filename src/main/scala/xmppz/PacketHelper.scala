package xmppz

object PacketHelper {

  def bindJid(jid: String): IQ = {
    IQ(iqType = "set",
      id = "bind_0",
      children = List(Bind(children = List[Packet](Jid(value = jid))))
    )
  }

  def bindSession(): IQ = {
    IQ(iqType = "set",
      id = "bind_1",
      children = List(Session()))
  }

  def getRoster(): IQ = {
    IQ(iqType = "get",
      id = "queryRoster2",
      children = List(Query(xmlns = "jabber:iq:roster")))

  }
}
