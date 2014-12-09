package com.github.mauricio.async.db.mysql.encoder

import com.github.mauricio.async.db.mysql.binary.BinaryRowEncoder
import com.github.mauricio.async.db.mysql.message.client.{ClientMessage, SendLongDataMessage}
import com.github.mauricio.async.db.util.ByteBufferUtils
import io.netty.buffer.{Unpooled, ByteBuf}

class SendLongDataEncoder( rowEncoder : BinaryRowEncoder ) extends MessageEncoder {

  def encode(message: ClientMessage): ByteBuf = {
    val m = message.asInstanceOf[SendLongDataMessage]

    val buffer = ByteBufferUtils.packetBuffer()
    buffer.writeByte(m.kind)
    buffer.writeBytes(m.statementId)
    buffer.writeShort(m.paramId)

    Unpooled.wrappedBuffer(buffer, encodeValue(m.value))
  }

  private def encodeValue( maybeValue: Any ) : ByteBuf = {
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