package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.mysql.message.server.ParamAndColumnProcessingFinishedMessage
import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import io.netty.buffer.ByteBuf

object ParamAndColumnProcessingFinishedDecoder : MessageDecoder {
    override fun decode(buffer: ByteBuf): ServerMessage {
        return ParamAndColumnProcessingFinishedMessage(EOFMessageDecoder.decode(buffer))
    }
}
