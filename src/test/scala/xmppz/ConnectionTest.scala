package xmppz
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import scala.concurrent.ExecutionContext
import java.util.concurrent.{ CountDownLatch, TimeUnit, Executors }
import java.io.PrintWriter
import scala.concurrent.Future
import xmppz._
import util._
import util.PromiseT._
import scalaz.WriterT
import scalaz.WriterT._
import scalaz._
import Scalaz._

@RunWith(classOf[JUnitRunner])
class ConnectionTest extends FunSuite {

  implicit val ec = ExecutionContext.fromExecutorService(Executors.newCachedThreadPool())

  val emptyLog = List[LogMsg[Connection]]()

  def getTestParams = ConnectionParams(AuthCredentials("", "", "", 3, ""),
    plumber = emptyReader(),
    incomingNoMatch = packet => {
      println("  -- no match " + packet)
      true
    }
  )

  test("Test Basic sendGet functionality") {
    val finishedLatch = new CountDownLatch(1)

    val conn = Connection(getTestParams, None)
    // format: OFF 
    val f =
      for {
        (conn, streamStart)     <-  conn.sendGet[StreamStart](StreamStart(domain = ""))
        _                       = println("ok got a stream start, running in thread " + Thread.currentThread())
        (conn, proceed)         <- conn.sendGet[ProceedTLS](StartTLS())
        _                       = println("OK got a proceed")
        (conn, streamStart) <- conn.sendGet[StreamStart](StreamStart(domain = ""))

      } yield {
        finishedLatch.countDown()
        true
      }
    // format: ON
    println("here, about to send the incoming events")
    Future {
      Connection.incoming(emptyLog, packetUpcaster(StreamStart(domain = "")))(conn.id)
      Thread.sleep(300)
      Connection.incoming(emptyLog, packetUpcaster(ProceedTLS()))(conn.id)
      Thread.sleep(300)
      Connection.incoming(emptyLog, packetUpcaster(StreamStart(domain = "")))(conn.id)
    }
    assert(finishedLatch.await(10, TimeUnit.SECONDS))
  }

  test("test connection logging") {
    val finishedLatch = new CountDownLatch(1)
    val conn = Connection(getTestParams, None)
        // format: OFF
        val cont = 
        for {
            (conn1, streamStart) <- conn.sendGet[StreamStart](StreamStart(domain=""))
            (conn2, proceed)     <- conn.sendGet[ProceedTLS](StartTLS())
            (conn3, proceed)     <- conn.sendGet[StreamFeatures](StreamFeatures())
            _   = finishedLatch.countDown()
        } yield { 
            conn3
        }
    //format: ON
    Future {
      Connection.incoming(emptyLog, packetUpcaster(StreamStart(domain = "")))(conn.id)
      Thread.sleep(100) //since this could outrun the other thread
      Connection.incoming(emptyLog, packetUpcaster(ProceedTLS()))(conn.id)
      Thread.sleep(100)
      Connection.incoming(emptyLog, packetUpcaster(StreamFeatures()))(conn.id)
      Thread.sleep(100)
    }
    finishedLatch.await()
    cont.handleLogs(l => assert(l.size == 3))
    cont.handleLogs(l => println(" ======> " + l))
  }

  def emptyReader() = new ConnectionPlumber {
    def run(f: (List[LogMsg[Connection]], Seq[Packet]) => Unit, ef: (List[LogMsg[Connection]], ConnectionError) => Unit) = println("running")

    def switchToTLS() = println("switch to tls")

    def write(str: String) = println("writing ") // + str)

    def shutDown() = println("shutting down fake plumber")
  }

  private def packetUpcaster(p: Packet): Packet = p
}
