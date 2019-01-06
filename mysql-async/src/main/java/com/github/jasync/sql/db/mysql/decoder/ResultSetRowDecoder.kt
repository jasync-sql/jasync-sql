package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.mysql.message.server.ResultSetRowMessage
import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import com.github.jasync.sql.db.util.ChannelWrapper
import com.github.jasync.sql.db.util.readBinaryLength
import io.netty.buffer.ByteBuf

class ResultSetRowDecoder : MessageDecoder {


    override fun decode(buffer: ByteBuf): ServerMessage {
        val row = ResultSetRowMessage()

        while (buffer.isReadable) {
            if (buffer.getUnsignedByte(buffer.readerIndex()) == ChannelWrapper.MySQL_NULL) {
                buffer.readByte()
                row.add(null)
            } else {
                val length = buffer.readBinaryLength().toInt()
                row.add(buffer.readBytes(length))
            }
        }

        return row
    }
}
