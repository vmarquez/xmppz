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
import xmppz.util.LogMsg
import java.util.concurrent.Executors
import org.jboss.netty.handler.ssl.SslHandler
import java.net.InetSocketAddress
import javax.net.ssl.{ SSLContext }
import scalaz._

import xmppz._

object NettyConnectionPlumber {

  def apply(addr: InetSocketAddress) = new ConnectionPlumber {

    var incomingEventHandler: Option[(List[LogMsg[Connection]], Seq[Packet]) => Unit] = None

    var incomingErrorHandler: Option[(List[LogMsg[Connection]], ConnectionError) => Unit] = None

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
    pipeline.addLast("handler", new XMLEventClientHandler(
      (l: List[LogMsg[Connection]], packets: Seq[Packet]) => incomingEventHandler.foreach(_(l, packets)),
      (l: List[LogMsg[Connection]], err: ConnectionError) => incomingErrorHandler.foreach(_(l, err))
    ))
    bootstrap.setPipelineFactory(new ChannelPipelineFactory {
      override def getPipeline: ChannelPipeline =
        pipeline
    })

    override def run(f: (List[LogMsg[Connection]], Seq[Packet]) => Unit, errorf: (List[LogMsg[Connection]], ConnectionError) => Unit): Unit = {
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

class XMLEventClientHandler(incoming: (List[LogMsg[Connection]], Seq[Packet]) => Unit, errorf: (List[LogMsg[Connection]], ConnectionError) => Unit) extends SimpleChannelUpstreamHandler {

  override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {}

  override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
    try {
      val m = e.getMessage()
      val events = m.asInstanceOf[\/[(List[LogMsg[Connection]], ConnectionError), (List[LogMsg[Connection]], Seq[Packet])]]
      events match {
        case \/-(tup) =>
          incoming(tup._1, tup._2)
        case -\/(tup) =>
          errorf(tup._1, tup._2)
      }
    } catch {
      case ex: Exception =>
        errorf(List[LogMsg[Connection]](), ConnectionError(message = "error in XMLEventClientHandler", exception = Some(ex))) //fixme: but what now?
    }
  }

  override def exceptionCaught(context: ChannelHandlerContext, e: ExceptionEvent) {
    println("Exception CAUGHT")
    e.getChannel.close()
  }
}

