import org.jboss.netty.bootstrap.ClientBootstrap
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel._
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.net.InetSocketAddress
import java.util.concurrent.Executors
import org.jboss.netty.handler.codec.frame.FrameDecoder

/**
 * User: Maurício Linhares
 * Date: 3/3/12
 * Time: 2:02 PM
 */
object HandlerExample extends App {

    var handler: HandlerExample = new HandlerExample
    Thread.sleep(3000)

}

object MessageDecoder extends FrameDecoder {
  def decode(ctx: ChannelHandlerContext, channel: Channel, buffer: ChannelBuffer): AnyRef = {
    buffer
  }
}

class HandlerExample extends SimpleChannelHandler {

  private var factory: NioClientSocketChannelFactory = null
  private var bootstrap: ClientBootstrap = null
  private var future: ChannelFuture = null

  this.factory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool, Executors.newCachedThreadPool)
  this.bootstrap = new ClientBootstrap(this.factory)

  this.bootstrap.setPipelineFactory(new ChannelPipelineFactory {
    def getPipeline: ChannelPipeline = {
      return Channels.pipeline( MessageDecoder, HandlerExample.this)
    }
  })

  this.bootstrap.setOption("child.tcpNoDelay", true)
  this.bootstrap.setOption("child.keepAlive", true)
  this.future = this.bootstrap.connect(new InetSocketAddress("localhost", 9999)).awaitUninterruptibly


  override def exceptionCaught(ctx: ChannelHandlerContext, e: ExceptionEvent): Unit = {
    System.out.printf("Exception caught: %s%n", e.getCause.getMessage)
    e.getCause.printStackTrace
    this.future.getChannel.disconnect
  }

}