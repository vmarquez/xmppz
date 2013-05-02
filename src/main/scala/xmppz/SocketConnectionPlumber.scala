package xmppz

import java.net.Socket
import scala.xml.pull._
import scala.io.Source
import java.io.{ BufferedInputStream, BufferedOutputStream, InputStreamReader, InputStream, BufferedReader, BufferedWriter, OutputStreamWriter }
import javax.net.ssl.{ SSLEngine, SSLContext, SSLSocket, X509TrustManager }
import java.security.cert.CertificateException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import util._

/*

  THIS ISN'T WORKING.  Could not get SSL to function correctly.  Suggestions? 
*/

//Mutability hurts my soul, maybe this should be a case class that always returns a new state, and the connection holding it would also copy...
object SocketConnectionPlumber {

  def apply(s: Socket) = new ConnectionPlumber {
    val originalSocket = s

    var reader = new BufferedReader(new InputStreamReader(s.getInputStream(), "UTF-8"))

    var writer = new BufferedWriter(new OutputStreamWriter(s.getOutputStream(), "UTF-8"))

    var ev: XMLEventReader = null

    private def getIterator(reader: BufferedReader) = new Iterator[Char] {
      override def hasNext = {
        val r = reader.ready()
        println("ready = " + r)
        r
      }

      override def next = {
        var c = reader.read().toChar
        print("BLAH" + c)
        c
      }
    }

    val torun = true

    override def run(f: Seq[Packet] => Unit, errorF: ConnectionError => Unit): Unit = {
      val incomingFunc = f
      ev = new XMLEventReader(new Source {
        val iter = getIterator(reader)
      })

      try {
        var events = List[XMLEvent]()
        val parser = XMLParser(PacketCreator(Some(incomingFunc)), true)
        while (torun) {
          val n = ev.next
          events = events :+ n
          parser.run(events) match {
            case Some((ev, packets)) =>
              incomingFunc(packets)
              events = List[XMLEvent]()
            case None =>
          }
        }
      } catch {
        case ex: Exception =>
          ex.printStackTrace()
      }
    }

    override def switchToTLS() {
      try {
        val context = SSLContext.getInstance("TLS")
        context.init(null, Array(X509Helper()), new java.security.SecureRandom())
        val sslSf = context.getSocketFactory()
        val socket =
          sslSf.createSocket(
            originalSocket,
            originalSocket.getInetAddress().getHostName(),
            originalSocket.getPort(),
            true)
        socket.setSoTimeout(0)
        socket.setKeepAlive(true)
        println("Strating handhsake")
        val sslSocket: SSLSocket = socket.asInstanceOf[SSLSocket]
        sslSocket.setUseClientMode(true)
        println("do you want to start the handshake?")

        reader = new BufferedReader(new InputStreamReader(sslSocket.getInputStream(), "UTF-8"))

        writer = new BufferedWriter(new OutputStreamWriter(sslSocket.getOutputStream(), "UTF-8"))

        ev = new XMLEventReader(new Source {
          val iter = getIterator(reader)
        })

        sslSocket.startHandshake() //this fails, someone help me?!?!?!
      } catch {
        case ex: Exception =>
          ex.printStackTrace()
      }
    }

    override def write(str: String): Unit = {
      writer.write(str)
      println("done writing")
    }

    override def shutDown(): Unit = {
      //fixme
      println("kill the socket")
    }
  }
}

object X509Helper {
  def apply() = new X509TrustManager {
    override def checkClientTrusted(chain: Array[X509Certificate], authType: String) =
      println("wooooooooo")

    override def checkServerTrusted(chain: Array[X509Certificate], authType: String) =
      println("CheckserverTRUuuuuusted authType = " + authType)

    override def getAcceptedIssuers() = Array[X509Certificate]()

  }
}

