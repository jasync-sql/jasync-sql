package com.github.mauricio.async.db.postgresql.encoders

import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import mu.KotlinLogging
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}

class PreparedStatementOpeningEncoder(val charset: Charset, val encoder : ColumnEncoderRegistry) : Encoder
  , PreparedStatementEncoderHelper
{

  import PreparedStatementOpeningEncoder.log

  override fun encode(message: ClientMessage): ByteBuf {

    val m = message as PreparedStatementOpeningMessage>

    val statementIdBytes = m.statementId.toString.getBytes(charset)
    val columnCount = m.valueTypes.size

    val parseBuffer = Unpooled.buffer(1024)

    parseBuffer.writeByte(ServerMessage.Parse)
    parseBuffer.writeInt(0)

    parseBuffer.writeBytes(statementIdBytes)
    parseBuffer.writeByte(0)
    parseBuffer.writeBytes(m.query.getBytes(charset))
    parseBuffer.writeByte(0)

    parseBuffer.writeShort(columnCount)

    if ( log.isDebugEnabled ) {
      log.debug(s"Opening query (${m.query}) - statement id (${statementIdBytes.mkString("-")}) - selected types (${m.valueTypes.mkString(", ")}) - values (${m.values.mkString(", ")})")
    }

    for (kind <- m.valueTypes) {
      parseBuffer.writeInt(kind)
    }

    ByteBufferUtils.writeLength(parseBuffer)

    val executeBuffer = writeExecutePortal(statementIdBytes, m.query, m.values, encoder, charset, true)

    Unpooled.wrappedBuffer(parseBuffer, executeBuffer)
  }

}