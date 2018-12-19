package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.mysql.message.server.PreparedStatementPrepareResponse
import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import io.netty.buffer.ByteBuf

class PreparedStatementPrepareResponseDecoder : MessageDecoder {


    override fun decode(buffer: ByteBuf): ServerMessage {

        //val dump = MySQLHelper.dumpAsHex(buffer)
        //log.debug("prepared statement response dump is \n{}", dump)

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
