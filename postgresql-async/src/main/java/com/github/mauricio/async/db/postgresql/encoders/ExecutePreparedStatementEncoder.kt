package com.github.mauricio.async.db.postgresql.encoders

import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.mauricio.async.db.postgresql.messages.frontend.ClientMessage
import com.github.mauricio.async.db.postgresql.messages.frontend.PreparedStatementExecuteMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class ExecutePreparedStatementEncoder(val charset: Charset, val encoder: ColumnEncoderRegistry) : Encoder, PreparedStatementEncoderHelper {

    override fun encode(message: ClientMessage): ByteBuf {

        val m = message as PreparedStatementExecuteMessage
        val statementIdBytes = m.statementId.toString().toByteArray(charset)

        return writeExecutePortal(statementIdBytes, m.query, m.values, encoder, charset)
    }

}
