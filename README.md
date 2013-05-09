## XMPPZ 

XMPPZ is a small xmpp library for scala.  The goal is an easy to use nonblocking API. 

### Usage

```scala
val conn = Connection.create(getConnParams("username", "password"))
val result =
      for {
        (conn, myjid)     <- ConnectionHelper.gchatConnect(conn, "xmppzExampleClient")
        (conn, presence)  <- conn.sendGet[Presence](Presence(from=Some(myjid), to=Some(tojid), presenceType=Some("probe")))
        conn              <- conn.send(Message(body=Some(msgtext), to=tojid, from=Some(myjid)))
        conn              <- conn.send(StreamEnd())
      } yield conn

```

Similar to Finalge, methods on connection return a Future allowing you to send and send and 'wait' for a particular packet type as the response. There are also methods
for awaiting, or sending asyncronously. When creating a connection object, you can also pass in a handler for unrequested incoming packets, so we are not constrained to the 
request/response model.  


### TODO

Quite a bit still to do, though the code is working.  

1. Move away from accumulating log messages with a list.  We need some mechanism that won't kill the heap for long running connections
2. Log incoming raw data from the EventDecoder along with logging packet creation with our XML Parser
3. Make the PacketCreator modular so anyone implementing additional RFCs can just accumulate a list of PFs that return their own Packets, do the same for toXML
4. More examples and boostrap logins for other common packet flows


