package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.mysql.message.server.OkMessage
import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import com.github.jasync.sql.db.util.readBinaryLength
import com.github.jasync.sql.db.util.readUntilEOF
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class OkDecoder(val charset: Charset) : MessageDecoder {

    override fun decode(buffer: ByteBuf): ServerMessage {

        return OkMessage(
            affectedRows = buffer.readBinaryLength(),
            lastInsertId = buffer.readBinaryLength(),
            statusFlags = buffer.readShort().toInt(),
            warnings = buffer.readShort().toInt(),
            message = buffer.readUntilEOF(charset)
        )

    }

}
