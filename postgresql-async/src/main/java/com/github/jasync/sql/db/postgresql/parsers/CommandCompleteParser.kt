package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.CommandCompleteMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.util.ByteBufferUtils
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset


class CommandCompleteParser(val charset: Charset) : MessageParser {

    override fun parseMessage(buffer: ByteBuf): ServerMessage {
        val result = ByteBufferUtils.readCString(buffer, charset)
        val indexOfRowCount = result.lastIndexOf(" ")
        val rowCount = if (indexOfRowCount == -1) {
            0
        } else {
            try {
                result.substring(indexOfRowCount).trim().toInt()
            } catch (e: NumberFormatException) {
                0
            }
        }

        return CommandCompleteMessage(rowCount, result)
    }

}
