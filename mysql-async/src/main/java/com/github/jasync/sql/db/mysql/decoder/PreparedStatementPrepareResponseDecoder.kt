package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.mysql.message.server.PreparedStatementPrepareResponse
import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import com.github.jasync.sql.db.util.BufferDumper.dumpAsHex
import io.netty.buffer.ByteBuf
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class PreparedStatementPrepareResponseDecoder : MessageDecoder {

    override fun decode(buffer: ByteBuf): ServerMessage {
        logger.trace { "prepared statement response dump is \n${dumpAsHex(buffer)}" }

        val statementId = ByteArray(4) { buffer.readByte() }
        val columnsCount = buffer.readUnsignedShort()
        val paramsCount = buffer.readUnsignedShort()

        // filler
        buffer.readByte()

        val warningCount = buffer.readShort()

        return PreparedStatementPrepareResponse(
            statementId = statementId,
            warningCount = warningCount,
            columnsCount = columnsCount,
            paramsCount = paramsCount
        )
    }
}
