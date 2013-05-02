package xmppz

import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;

import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.ChannelPipelineFactory
import org.jboss.netty.handler.codec.string.StringDecoder
import org.jboss.netty.handler.codec.string.StringEncoder
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicLong
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{
  ChannelEvent,
  ChannelHandlerContext,
  ChannelStateEvent,
  ExceptionEvent,
  MessageEvent,
  Channel,
  Channels,
  SimpleChannelUpstreamHandler
}

import util._
import netty._

class TestNettyServer(port: Int) {

  val bootstrap = new ServerBootstrap(
    new NioServerSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool()))

  val handler = new SimpleServerHandler

  var channel: Option[Channel] = None

  bootstrap.setPipelineFactory(new ChannelPipelineFactory {
    override def getPipeline = Channels.pipeline(
      new StringDecoder,
      new StringEncoder,
      new SimpleServerHandler)
  })
  channel = Some(bootstrap.bind(new InetSocketAddress(port)))
  println("listneing on port " + port)

  def writeToClient(str: String) =
    handler.writeToClient(str)

  var clientChannel: Option[Channel] = None

  class SimpleServerHandler extends SimpleChannelUpstreamHandler {

    def writeToClient(str: String) {
      for (client <- clientChannel)
        client.write(str)
    }

    override def channelConnected(ctx: ChannelHandlerContext, e: ChannelStateEvent) {
      clientChannel = Some(e.getChannel())
    }

    override def messageReceived(ctx: ChannelHandlerContext, e: MessageEvent) {
      // Send back the received message to the remote peer.
      e.getChannel.write(e.getMessage.toString.reverse)
    }

    override def exceptionCaught(context: ChannelHandlerContext, e: ExceptionEvent) {
      // Close the connection when an exception is raised.
      e.getChannel.close()
    }

  }

  def close() {
    channel.foreach(_.close())
  }
}
