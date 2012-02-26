package com.github.mauricio.postgresql

import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory
import java.util.concurrent.Executors
import org.jboss.netty.bootstrap.ClientBootstrap
import java.net.InetSocketAddress
import scala.collection.JavaConversions
import collection.Map
import org.jboss.netty.channel.{ChannelFuture, Channels, ChannelPipeline, ChannelPipelineFactory}

/**
 * User: Maurício Linhares
 * Date: 2/25/12
 * Time: 7:46 PM
 */

object Connection {

  val Name = "Netty-PostgreSQL-driver-0.0.1"

}

class Connection(host: String, port: Int, username: String, password: String, database: String) {

  val factory = new NioClientSocketChannelFactory(Executors.newCachedThreadPool(), Executors.newCachedThreadPool());
  val handler = new DatabaseConnectionHandler( username , database)
  val bootstrap = new ClientBootstrap(this.factory)
  var channelFuture : ChannelFuture  = null

  def connect : Unit = {

    this.bootstrap.setPipelineFactory(new ChannelPipelineFactory() {

      override def getPipeline(): ChannelPipeline = {
        return Channels.pipeline(new MessageDecoder(), handler);
      }

    });

    this.bootstrap.setOption("child.tcpNoDelay", true);
    this.bootstrap.setOption("child.keepAlive", true);
    this.channelFuture = this.bootstrap.connect(new InetSocketAddress( this.host, this.port)).awaitUninterruptibly()

  }

  def disconnect : Unit = {
    this.channelFuture.getChannel.getCloseFuture.awaitUninterruptibly()
  }

  def parameterStatuses : Map[String, String] = {
    JavaConversions.mapAsScalaMap(this.handler.parameterStatus)
  }

}
