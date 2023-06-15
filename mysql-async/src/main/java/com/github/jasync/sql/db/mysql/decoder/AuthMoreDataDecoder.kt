package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.mysql.message.server.AuthMoreDataMessage
import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import io.netty.buffer.ByteBuf

class AuthMoreDataDecoder : MessageDecoder {
    override fun decode(buffer: ByteBuf): ServerMessage {
        return AuthMoreDataMessage(
            data = buffer.readByte()
        )
    }
}
