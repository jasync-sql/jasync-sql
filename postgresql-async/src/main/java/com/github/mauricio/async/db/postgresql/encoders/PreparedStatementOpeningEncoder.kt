package com.github.mauricio.async.db.postgresql.encoders

import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import com.github.mauricio.async.db.postgresql.messages.frontend.ClientMessage
import com.github.mauricio.async.db.postgresql.messages.frontend.PreparedStatementOpeningMessage
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import mu.KotlinLogging
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}

class PreparedStatementOpeningEncoder(val charset: Charset, val encoder : ColumnEncoderRegistry) : Encoder
  , PreparedStatementEncoderHelper
{


  override fun encode(message: ClientMessage): ByteBuf {

    val m = message as PreparedStatementOpeningMessage

    val statementIdBytes = m.statementId.toString().toByteArray(charset)
    val columnCount = m.valueTypes.size

    val parseBuffer = Unpooled.buffer(1024)

    parseBuffer.writeByte(ServerMessage.Parse)
    parseBuffer.writeInt(0)

    parseBuffer.writeBytes(statementIdBytes)
    parseBuffer.writeByte(0)
    parseBuffer.writeBytes(m.query.toByteArray(charset))
    parseBuffer.writeByte(0)

    parseBuffer.writeShort(columnCount)

    if ( logger.isDebugEnabled ) {
      logger.debug("Opening query (${m.query}) - statement id (${statementIdBytes.joinToString("-")}) - selected types (${m.valueTypes.joinToString(", ")}) - values (${m.values.joinToString(", ")})")
    }

    for (kind in m.valueTypes) {
      parseBuffer.writeInt(kind)
    }

    ByteBufferUtils.writeLength(parseBuffer)

    val executeBuffer = writeExecutePortal(statementIdBytes, m.query, m.values, encoder, charset, true)

    return Unpooled.wrappedBuffer(parseBuffer, executeBuffer)
  }

}
