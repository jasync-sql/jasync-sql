package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.ParameterStatusMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import com.github.jasync.sql.db.util.ByteBufferUtils.readCString
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset


class ParameterStatusParser(val charset: Charset) : MessageParser {

    override fun parseMessage(buffer: ByteBuf): ServerMessage {
        return ParameterStatusMessage(readCString(buffer, charset), readCString(buffer, charset))
    }

}
