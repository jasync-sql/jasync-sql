package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.mysql.message.server.EOFMessage
import io.netty.buffer.ByteBuf

object EOFMessageDecoder : MessageDecoder {

    override fun decode(buffer: ByteBuf): EOFMessage {
        return EOFMessage(
            buffer.readUnsignedShort(),
            buffer.readUnsignedShort()
        )
    }
}
