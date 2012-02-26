import java.net.InetSocketAddress
import org.jboss.netty.channel.ChannelPipelineFactory
import java.util.concurrent.Executors
import org.jboss.netty.channel.ChannelPipeline
import org.jboss.netty.channel.Channels
import org.jboss.netty.bootstrap.ClientBootstrap
import com.github.mauricio.postgresql.DatabaseConnectionHandler
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import com.github.mauricio.postgresql.MessageDecoder

object DatabaseConnectionTest extends App {

  val factory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());

  val bootstrap = new ClientBootstrap(factory);
  bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

    override def getPipeline(): ChannelPipeline = {
      return Channels.pipeline( new MessageDecoder(), new DatabaseConnectionHandler( "postgres", "postgres" ) );
    }

  });

  bootstrap.setOption("child.tcpNoDelay", true);
  bootstrap.setOption("child.keepAlive", true);

  bootstrap.connect(new InetSocketAddress( "localhost", 5433)).awaitUninterruptibly()

}