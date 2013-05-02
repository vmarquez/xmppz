package xmppz.netty

import org.jboss.netty.channel.{
  DefaultChannelPipeline,
  Channel,
  ChannelFuture,
  ChannelPipeline,
  Channels,
  ChannelPipelineFactory,
  ChannelHandlerContext,
  SimpleChannelUpstreamHandler,
  MessageEvent,
  ChannelStateEvent,
  ExceptionEvent
}
import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import org.jboss.netty.handler.codec.string.StringEncoder

import org.jboss.netty.handler.codec.frame.FrameDecoder
import org.jboss.netty.buffer.ChannelBuffer
import java.io.{ PipedInputStream, PipedOutputStream }
import scala.io.Source
import scala.xml.pull.XMLEventReader
import scala.xml.pull.XMLEvent
import java.util.concurrent.Executors
import org.jboss.netty.handler.ssl.SslHandler
import java.net.InetSocketAddress
import javax.net.ssl.{ SSLEngine, SSLContext }
import java.lang.StringBuilder
import scalaz._
import Scalaz._

import xmppz._

object NettyConnectionPlumber {

  def apply(addr: InetSocketAddress) = new ConnectionPlumber {

    var incomingEventHandler: Option[Seq[Packet] => Unit] = None

    var incomingErrorHandler: Option[ConnectionError => Unit] = None

    val bootstrap = new ClientBootstrap(new NioClientSocketChannelFactory(Executors.newCachedThreadPool, Executors.newCachedThreadPool))

    var channel: Option[Channel] = None

    val context = SSLContext.getInstance("TLS")
    context.init(null, null, null)
    val engine = context.createSSLEngine()

    engine.setUseClientMode(true)
    val sslhandler = new SslHandler(engine)
    sslhandler.setEnableRenegotiation(true)
    //val eventGenerator = new EventGenerator(incoming)

    val pipeline = new DefaultChannelPipeline()
    val eventDecoder = new EventDecoder(PacketCreator())
    pipeline.addLast("eventdecoder", eventDecoder)
    pipeline.addLast("stringencoder", new StringEncoder)
    pipeline.addLast("handler", new XMLEventClientHandler((packets: Seq[Packet]) =>
      incomingEventHandler.foreach(_(packets)),
      (err: ConnectionError) => incomingErrorHandler.foreach(_(err))
    ))

    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      override def getPipeline: ChannelPipeline =
        pipeline
    })

    override def run(f: Seq[Packet] => Unit, errorf: ConnectionError => Unit): Unit = {
      incomingEventHandler = Some(f)
      incomingErrorHandler = Some(errorf)
      val future: ChannelFuture = bootstrap.connect(addr)

      if (!future.await(1000))
        throw new Exception("WTF WHY CAN'T WE CONNECT, Timeout for bootstrap connect") //client should blow up if we don't get connected fast enough
      channel = Some(future.getChannel())
    }

    override def switchToTLS() {
      println("starting TLS")

      pipeline.addFirst("sslhandler", sslhandler)
      val cfuture = sslhandler.handshake()
      Thread.sleep(1000) //work around
      //cfuture.await()// Uhh, bug here with netty? definitely getting threading errors using their future 
    }

    override def write(s: String): Unit = {
      try {
        channel.foreach(_.write(s))
      } catch {
        case ex: Exception => ex.printStackTrace()
      }
    }

    override def shutDown(): Unit = {
      eventDecoder.shutdown()
      channel.foreach(_.close().awaitUninterruptibly())
      bootstrap.releaseExternalResources();
    }
  }
}

class XMLEventClientHandler(incoming: Seq[Packet] => Unit, errorf: ConnectionError => Unit) extends SimpleChannelUpstreamHandler {

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {}

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    try {
      val m = e.getMessage()
      val events = m.asInstanceOf[\/[ConnectionError, Seq[Packet]]]
      events match {
        case \/-(p) =>
          incoming(p)
        case -\/(err) =>
          println("error = " + err)
          errorf(err)
      }
    } catch {
      case ex: Exception =>
        errorf(ConnectionError(message = "error in XMLEventClientHandler", exception = Some(ex))) //fixme: but what now?
    }
  }

  override def exceptionCaught(context: ChannelHandlerContext, e: ExceptionEvent) {
    println("Exception CAUGHT")
    e.getChannel.close()
  }
}

