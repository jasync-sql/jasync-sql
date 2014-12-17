package com.github.mauricio.async.db.mysql.codec

import com.github.mauricio.async.db.mysql.message.client.{ClientMessage, SendLongDataMessage}
import com.github.mauricio.async.db.util.{ByteBufferUtils, Log}
import io.netty.buffer.Unpooled
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageEncoder

object SendLongDataEncoder {
  val log = Log.get[SendLongDataEncoder]

  val LONG_THRESHOLD = 1023
}

class SendLongDataEncoder
    extends MessageToMessageEncoder[SendLongDataMessage](classOf[SendLongDataMessage]) {

  import com.github.mauricio.async.db.mysql.codec.SendLongDataEncoder.log

  def encode(ctx: ChannelHandlerContext, message: SendLongDataMessage, out: java.util.List[Object]): Unit = {
    if ( log.isTraceEnabled ) {
      log.trace(s"Writing message ${message.toString}")
    }

    val sequence = 0

    val headerBuffer = ByteBufferUtils.mysqlBuffer(3 + 1 + 1 + 4 + 2)
    ByteBufferUtils.write3BytesInt(headerBuffer, 1 + 4 + 2 + message.value.readableBytes())
    headerBuffer.writeByte(sequence)

    headerBuffer.writeByte(ClientMessage.PreparedStatementSendLongData)
    headerBuffer.writeBytes(message.statementId)
    headerBuffer.writeShort(message.paramId)

    val result = Unpooled.wrappedBuffer(headerBuffer, message.value)

    out.add(result)
  }

}
