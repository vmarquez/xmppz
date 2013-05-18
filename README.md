## XMPPZ 

XMPPZ is a small xmpp library for scala.  The goal is an easy to use nonblocking, functional API.  By default, XMPPZ uses netty for underlying network IO.

### Sample Usage 

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

XMPPZ lets you create connections and execute future based request/responses with xmpp packets.  Included is a small working example for sending messages called [GchatQuickMsg](https://github.com/vmarquez/xmppz/blob/master/src/main/scala/xmppz/example/GchatQuickMsg.scala):


### Step by step

To get started, you must create a connection instance that will represent an XMPP session for a particular user, accomplished by passing in a ConnectionParam struct to the create method.
Along with authorziation credentials and server information, we must also instantiate a [ConnectionPlumber]() instance to handle IO.  Currently we are using Netty.
All ConnectionPlumbers must also take a function for handling incoming unrequested data.  Incoming messages, for example are an example of unrequested data, and must
return true if they were able to successfully handle the incoming data.  



```scala
def getConnParams(user: String, pass: String): ConnectionParams = {
    val encoded = SASLHandler.getAuthTextPlain(user, pass, host)
    ConnectionParams(AuthCredentials(user, "gmail.com", host, port, encoded),
      plumber = NettyConnectionPlumber(new InetSocketAddress(host, port)),
      incomingNoMatch = packet => {
        println("  unrequested packets= " + packet.getClass)
        true //this indicates we've successfully handled this packet type, and there is no need to look for a corresponding request 
      })
  }
```

Connetion instances support three separate methods: send, sendGet, and get, the latter two being asynchronous and awaiting the appropriate polymorphic packet type. 
A detailed example of the various uses of these are found in the [ConnectionHelper](https://github.com/vmarquez/xmppz/blob/master/src/main/scala/xmppz/ConnectionHelper.scala) class.

```scala
    for {
      conn                    <- conn.send("<?xml version='1.0'?>") //you can send a packet or raw XML with the send method
      (conn, streamStart)     <- conn.sendGet[StreamStart](StreamStart(domain = conn.p.authParams.domain)) //if another packet returns and is not handled by the function passed to the plumber, this will return a '\/.left' with an error
      (conn, streamFeatures)  <- conn.get[StreamFeatures]
      (conn, proceed)         <- if (streamFeatures.starttls)
                                    conn.sendGet[ProceedTLS](StartTLS())
                                else
                                    Connection.error(conn, "Server did not want TLS") //an error will stop comprehension. 
       conn                   <- Connection.secureTLS(conn)
      (conn, streamStart)     <- conn.sendGet[StreamStart](StreamStart(domain = conn.p.authParams.domain))
      (conn, streamFeatures)  <- conn.get[StreamFeatures]
      (conn, success)         <- conn.sendGet[SASLSuccess](new Auth(mechanism = "PLAIN", encoded = conn.p.authParams.password))
      (conn, streamStart)     <- conn.sendGet[StreamStart](StreamStart(domain = conn.p.authParams.domain))
      (conn, streamFeatures)  <- conn.get[StreamFeatures]
      (conn, iq)              <- conn.sendGet[IQ](PacketHelper.bindJid(conn.p.authParams.jid))
      myjid                   = iq.collect { case jid: Jid => jid } //collect is a utility function to easily traverse packets 
                                .map(jid => jid.value)
                                .getOrElse("")
      (conn, iq)              <- conn.sendGet[IQ](PacketHelper.bindSession())
      (conn, roster)          <- conn.sendGet[Roster](PacketHelper.getRoster())
      conn                    <- conn.send(Presence(priority = 1, status = Some(presenceStatus), id = Some("fixme?")))
      } yield {
        (conn, myjid)  
      }
```

Logging is done at every step to help with debugging and/or programatic inspection.  In our first bit of code, we assinged the result of our cmprehension to a value named 'result'. 
Here, we show how to print the logs generated to the screen:

```scala
result.handleLogs(logs =>
      logs.foreach { log =>
        println("Thread [" + log.thread + "] Object [" + log.caller.map(c => c.p.authParams.jid + c.hashCode).getOrElse("") + "] msg=[" + log.msg + "]")
      }
    )
```



### TODO

Quite a bit still to do, though the code is working.  

1. Move away from accumulating log messages with a list.  We need some mechanism that won't kill the heap for long running connections
4. More examples and boostrap logins for other common packet flows


