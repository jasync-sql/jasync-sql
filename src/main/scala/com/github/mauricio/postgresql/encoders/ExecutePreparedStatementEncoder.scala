package com.github.mauricio.postgresql.encoders

import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.{ChannelUtils, CharsetHelper}
import com.github.mauricio.postgresql.column.ColumnEncoderDecoder
import com.github.mauricio.postgresql.messages.frontend.{FrontendMessage, PreparedStatementExecuteMessage}
import com.github.mauricio.postgresql.messages.backend.Message

/**
 * User: Maurício Linhares
 * Date: 3/12/12
 * Time: 6:45 PM
 */

object ExecutePreparedStatementEncoder extends Encoder {
  def encode(message: FrontendMessage): ChannelBuffer = {

    val m = message.asInstanceOf[PreparedStatementExecuteMessage]

    val queryBytes = CharsetHelper.toBytes(m.query)

    val bindBuffer = ChannelBuffers.dynamicBuffer(1024)

    bindBuffer.writeByte(Message.Bind)
    bindBuffer.writeInt(0)

    bindBuffer.writeBytes(queryBytes)
    bindBuffer.writeByte(0)
    bindBuffer.writeBytes(queryBytes)
    bindBuffer.writeByte(0)

    bindBuffer.writeShort(0)

    bindBuffer.writeShort(m.values.length)

    for ( value <- m.values ) {
      if ( value == null ) {
        bindBuffer.writeInt(-1)
      } else {
        val encoded = ColumnEncoderDecoder.encode(value).getBytes( CharsetHelper.Unicode )
        bindBuffer.writeInt(encoded.length)
        bindBuffer.writeBytes( encoded )
      }
    }

    bindBuffer.writeShort(0)

    ChannelUtils.writeLength(bindBuffer)

    val executeLength = 1 + 4 + queryBytes.length + 1 + 4
    val executeBuffer = ChannelBuffers.buffer( executeLength )
    executeBuffer.writeByte(Message.Execute)
    executeBuffer.writeInt(executeLength - 1)

    executeBuffer.writeBytes(queryBytes)
    executeBuffer.writeByte(0)

    executeBuffer.writeInt(0)

    val closeLength = 1 + 4 + 1 + queryBytes.length + 1
    val closeBuffer = ChannelBuffers.buffer(closeLength)
    closeBuffer.writeByte(Message.CloseStatementOrPortal)
    closeBuffer.writeInt( closeLength - 1 )
    closeBuffer.writeByte('P')

    closeBuffer.writeBytes(queryBytes)
    closeBuffer.writeByte(0)

    val syncBuffer = ChannelBuffers.buffer(5)
    syncBuffer.writeByte(Message.Sync)
    syncBuffer.writeInt(4)

    ChannelBuffers.wrappedBuffer(bindBuffer, executeBuffer, syncBuffer, closeBuffer)

  }
}
