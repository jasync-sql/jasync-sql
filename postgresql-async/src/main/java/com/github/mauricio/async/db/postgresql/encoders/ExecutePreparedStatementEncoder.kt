package com.github.mauricio.async.db.postgresql.encoders

import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.mauricio.async.db.postgresql.messages.frontend.ClientMessage
import com.github.mauricio.async.db.postgresql.messages.frontend.PreparedStatementExecuteMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class ExecutePreparedStatementEncoder(charset: Charset, encoder: ColumnEncoderRegistry) : Encoder, PreparedStatementEncoderHelper {

  fun encode(message: ClientMessage): ByteBuf {

    val m = message as PreparedStatementExecuteMessage
    val statementIdBytes = m.statementId.toString.getBytes(charset)

    writeExecutePortal(statementIdBytes, m.query, m.values, encoder, charset)
  }

}