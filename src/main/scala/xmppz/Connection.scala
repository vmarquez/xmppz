package xmppz

import scala.concurrent._
import scala.io.Source
import java.util.concurrent.{ ConcurrentHashMap }
import java.io._
import scala.collection._
import scalaz.effect.IO
import scala.xml.pull._
import scalaz.{ Monad, EitherT }
import scalaz.EitherT._
import scalaz._
import scalaz.\/._
import Scalaz._
import xmppz._
import util._
import util.Log._
import packet.PacketOutput

//TODO: we need to fix logging so we don't blow the heap for long running stuff. List is bad...
object Connection {

  val connMap = new ConcurrentHashMap[String, Connection]()

  def create(p: ConnectionParams)(implicit output: PacketOutput): Connection = {
    val incomingFunc: (List[LogMsg[Connection]], Seq[Packet]) => Unit = (logs, incomingPackets) => {
      incomingPackets.headOption.foreach(incoming(logs, _)(p.authParams.toString))
      incomingPackets.safeTail.foreach(incoming(List[LogMsg[Connection]](), _)(p.authParams.toString))
    }
    //TODO: handle logging here too
    val incomingError: (List[LogMsg[Connection]], ConnectionError) => Unit = (logs, error) => Connection.incomingError(error, p.authParams.toString)

    try {
      p.plumber.run(incomingFunc, incomingError)

    } catch {
      case ex: Exception =>
        println("exception in future = " + ex)
        ex.printStackTrace()
    }
    val conn = Connection(p = p, output = output)
    connMap.put(conn.id, conn)
    conn
  }

  protected[xmppz] def incomingError(error: ConnectionError, connectionHash: String): Unit = {
    try {
      val conn = connMap.get(connectionHash)
      val condition = conn.condition
      val newconn = conn.copy(condition = None)
      updateConn(newconn) //better way to handlet his?
      condition match {
        case Some(packetCondition) =>
          //fixme: someone save me from this awfulness. ideas?
          val pc = packetCondition.promise.asInstanceOf[ThreadlessPromise[\/[(Connection, ConnectionError), (Connection, Packet)]]]
          pc.success((conn, error).left)
        case None =>
        //wtf? should i even log an error here? i suppose...
      }
    } catch {
      case ex: Exception =>
        println("fatal exception = " + ex)
    }
  }

  //we might want a way to timeout some of these 
  protected[xmppz] def incoming[T <: Packet](log: List[LogMsg[Connection]], packet: T)(connectionHash: String): Unit = {
    try {
      val conn = connMap.get(connectionHash)
      val m = Manifest.singleType(packet)
      //println("      incoming packet = " + packet + " condition = " + conn.condition + " conn id = " + conn.id + "Trhead =" + Thread.currentThread())

      val cond = conn.condition
      val newconn = conn.copy(condition = None)
      updateConn(newconn)
      cond match {
        case Some(packetCondition) if (packetCondition.manifest == m) =>
          //gross, casting.  HELP!
          val pc = packetCondition.promise.asInstanceOf[ThreadlessPromise[(List[LogMsg[Connection]], \/[(Connection, ConnectionError), (Connection, T)])]]
          //println(connectionHash + " received a " + packet.toString)
          pc.success((List[LogMsg[Connection]](packetCondition.log) ::: log, (newconn, packet).right)) //append more things to the log!

        case Some(packetCondition) =>
          if (!conn.p.incomingNoMatch(packet)) {
            val log = Log("warn", "we didn't find " + packetCondition.manifest.toString, conn)

            packetCondition.promise.success((List[LogMsg[Connection]](log),
              (newconn, ConnectionError(message = "Didn't get what we expected, ended up with" + packet)).left))
          }
        case None =>
          if (!conn.p.incomingNoMatch(packet))
            println("NONE???, condition = " + cond) //something went very wrong here...how to handle this error? 
      }

    } catch {
      case ex: Exception =>
        println("FATAL ERROR for connection " + connectionHash + " we can't find it") //again, something went very wrong....
    }
  }

  //Fixme: take an error object.  Also, do we want another one that overloads to \/[(Connection, ConnectionError), Connection]
  def error(conn: Connection, str: String): EitherT[WriterFuture, (Connection, ConnectionError), (Connection, Packet)] = {
    val promise = new ThreadlessPromise[(List[LogMsg[Connection]], \/[(Connection, ConnectionError), (Connection, Packet)])]
    promise.success((List[LogMsg[Connection]](Log("Error", str, conn)), (conn, ConnectionError(message = str)).left))

    getTransformer(promise.future)
  }

  private def updateConn(conn: Connection) =
    connMap.put(conn.id, conn)

  def secureTLS(conn: Connection): EitherT[WriterFuture, (Connection, ConnectionError), Connection] = {
    val future: Future[(List[LogMsg[Connection]], \/[(Connection, ConnectionError), Connection])] = Future {
      conn.p.plumber.switchToTLS()
      val log = Log("info", "securing TLS", conn)
      (List[LogMsg[Connection]](log), conn.right)
    }
    getTransformer(future)
  }

}
/*
Thought about making Connection itself a monad instead of stacking the transformers. Might making logging easier if 
it itself held LogMsgs.  
The methods all return Future[Writer[(List[LogMsg], \/[(Connection,ConnectionError),B]]] but in Transformer form it's
EitherT[WriterT[Future...]...]
*/
case class Connection(p: ConnectionParams,
    condition: Option[PacketCondition[_ <: Packet]] = None,
    output: PacketOutput) {

  def sendGet[T <: Packet](packet: Packet)(implicit m: Manifest[T]): EitherT[WriterFuture, (Connection, ConnectionError), (Connection, T)] = {
    val promise = new ThreadlessPromise[(List[LogMsg[Connection]], \/[(Connection, ConnectionError), (Connection, T)])]
    val log = Log("trace", "sendGet[" + m + "](" + packet + ")", this)
    val packetcondition = PacketCondition(m, log, promise)
    val newconn = copy(condition = Some(packetcondition))
    Connection.connMap.put(id, newconn)
    val packetToWrite = output.toXMLString(packet)
    p.plumber.write(packetToWrite)
    getTransformer(promise)
  }

  def get[T <: Packet](implicit m: Manifest[T]): EitherT[WriterFuture, (Connection, ConnectionError), (Connection, T)] = {
    val promise = new ThreadlessPromise[(List[LogMsg[Connection]], \/[(Connection, ConnectionError), (Connection, T)])]
    val log = Log("trace", "get[" + m + "]", this)
    val packetcondition = PacketCondition(m, log, promise)
    Connection.connMap.put(p.authParams.toString, copy(condition = Some(packetcondition)))
    getTransformer(promise)
  }

  def send(data: String): EitherT[WriterFuture, (Connection, ConnectionError), Connection] = {
    println("here we go, sending " + data)
    val promise = new ThreadlessPromise[(List[LogMsg[Connection]], \/[(Connection, ConnectionError), Connection])]
    val log = Log("trace", "send(" + data + ")", this)
    promise.success((List[LogMsg[Connection]](log), this.right))
    p.plumber.write(data)
    getTransformer(promise)
  }

  def send[T <: Packet](p: Packet): EitherT[WriterFuture, (Connection, ConnectionError), Connection] = send(output.toXMLString(p)) ///send(Packet.toXmlString(p))

  protected[xmppz] def id: String = p.authParams.toString()
}

case class ConnectionParams(
  authParams: AuthCredentials,
  plumber: ConnectionPlumber,
  incomingNoMatch: Packet => Boolean)

case class PacketCondition[T <: Packet](
  manifest: Manifest[T],
  log: LogMsg[Connection],
  promise: ThreadlessPromise[(List[LogMsg[Connection]], \/[(Connection, ConnectionError), (Connection, T)])])

