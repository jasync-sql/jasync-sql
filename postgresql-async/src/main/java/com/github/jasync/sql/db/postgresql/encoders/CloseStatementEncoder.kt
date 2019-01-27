package com.github.jasync.sql.db.postgresql.encoders

import com.github.jasync.sql.db.postgresql.messages.frontend.ClientMessage
import com.github.jasync.sql.db.postgresql.messages.frontend.PreparedStatementCloseMessage
import io.netty.buffer.ByteBuf
import mu.KotlinLogging
import java.nio.charset.Charset

private val logger = KotlinLogging.logger {}

class CloseStatementEncoder(val charset: Charset) : Encoder, PreparedStatementEncoderHelper {

    override fun encode(message: ClientMessage): ByteBuf {
        val m = message as PreparedStatementCloseMessage

        if ( logger.isDebugEnabled ) {
            logger.debug("Closing statement ({})", m)
        }

        return closeAndSyncBuffer(m.statementId.toString().toByteArray(charset), PreparedStatementEncoderHelper.Statement)
    }

}
