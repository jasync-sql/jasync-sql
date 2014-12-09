package com.github.mauricio.async.db.mysql.codec

import java.nio.charset.Charset

import com.github.mauricio.async.db.mysql.binary.BinaryRowEncoder
import com.github.mauricio.async.db.mysql.message.client.{ClientMessage, SendLongDataMessage}
import com.github.mauricio.async.db.util.{Log, ByteBufferUtils}
import io.netty.buffer.{Unpooled, ByteBuf}
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

object SendLongDataEncoder {
  val log = Log.get[SendLongDataEncoder]
}

class SendLongDataEncoder(charset: Charset)
    extends MessageToMessageEncoder[SendLongDataMessage](classOf[SendLongDataMessage]) {

  import com.github.mauricio.async.db.mysql.codec.SendLongDataEncoder.log

  private final val rowEncoder = new BinaryRowEncoder(charset)

  def isLong(value: Any): Boolean = rowEncoder.encoderFor(value).isLong(value)

  def encode(ctx: ChannelHandlerContext, message: SendLongDataMessage, out: java.util.List[Object]): Unit = {
    val result: ByteBuf = encode(message)

    ByteBufferUtils.writePacketLength(result, 0)

    if ( log.isTraceEnabled ) {
      log.trace(s"Writing message ${message.toString}")
    }

    out.add(result)
  }

  private def encode(message: SendLongDataMessage): ByteBuf = {
    val buffer = ByteBufferUtils.packetBuffer()
    buffer.writeByte(ClientMessage.PreparedStatementSendLongData)
    buffer.writeBytes(message.statementId)
    buffer.writeShort(message.paramId)

    Unpooled.wrappedBuffer(buffer, encodeValue(message.value))
  }

  private def encodeValue(maybeValue: Any) : ByteBuf = {
    if ( maybeValue == null || maybeValue == None ) {
      throw new UnsupportedOperationException("Cannot encode NULL as long value")
    } else {
      val value = maybeValue match {
        case Some(v) => v
        case _ => maybeValue
      }
      val encoder = rowEncoder.encoderFor(value)
      encoder.encodeLong(value)
    }
  }

}
