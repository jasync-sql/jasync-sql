package com.github.mauricio.async.db.postgresql.encoders

import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.mauricio.async.db.postgresql.messages.frontend.ClientMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class ExecutePreparedStatementEncoder(charset: Charset, encoder: ColumnEncoderRegistry) : Encoder
with PreparedStatementEncoderHelper
{

  fun encode(message: ClientMessage): ByteBuf {

    val m = message.asInstanceOf[PreparedStatementExecuteMessage]
    val statementIdBytes = m.statementId.toString.getBytes(charset)

    writeExecutePortal(statementIdBytes, m.query, m.values, encoder, charset)
  }

}