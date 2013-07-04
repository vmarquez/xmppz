package xmppz

import util._
import scalaz._
import Scalaz._
import xmppz._
import scala.concurrent.ExecutionContext
import packet._
object ConnectionHelper {
  import packet.CorePacket._
  // format: OFF
  def saslConnect(conn: Connection, presenceStatus: String)(implicit ec: ExecutionContext) =
    for {
      conn                    <- conn.send("<?xml version='1.0'?>")
      (conn, streamStart)     <- conn.sendGet[StreamStart](StreamStart(domain = conn.p.authParams.domain))
      (conn, streamFeatures)  <- conn.get[StreamFeatures]
      (conn, proceed)         <- if (streamFeatures.starttls)
                                    conn.sendGet[ProceedTLS](StartTLS())
                                else
                                    Connection.error(conn, "Server did not want TLS") //we could use an impicit for this kind of thing but...???
       conn                   <- Connection.secureTLS(conn)
      (conn, streamStart)     <- conn.sendGet[StreamStart](StreamStart(domain = conn.p.authParams.domain))
      (conn, streamFeatures)  <- conn.get[StreamFeatures]
      (conn, success)         <- conn.sendGet[SASLSuccess](new Auth(mechanism = "PLAIN", encoded = conn.p.authParams.password))
      (conn, streamStart)     <- conn.sendGet[StreamStart](StreamStart(domain = conn.p.authParams.domain))
      (conn, streamFeatures)  <- conn.get[StreamFeatures]
      (conn, iq)              <- conn.sendGet[IQ](bindJid(conn.p.authParams.jid))
      myjid                   = iq.collect { case jid: Jid => jid } //uh, we might want to lift this so we can return an error if we can't find it
                                .map(jid => jid.value)
                                .getOrElse("")
      (conn, iq)              <- conn.sendGet[IQ](bindSession())
      (conn, roster)          <- conn.sendGet[Roster](getRoster())
      conn                    <- conn.send(Presence(priority = 1, status = Some(presenceStatus), id = Some("fixme?")))
      //todo: check if stream features requires a session?
      } yield {
        (conn, myjid) //anything else we'll need most likely? 
      }
}

