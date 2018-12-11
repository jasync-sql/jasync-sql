package com.github.jasync.sql.db.mysql.binary.decoder

import com.github.jasync.sql.db.util.readBinaryLength
import io.netty.buffer.ByteBuf

object ByteArrayDecoder : BinaryDecoder {
    override fun decode(buffer: ByteBuf): Any {
        val length = buffer.readBinaryLength()
        val bytes = ByteArray(length.toInt())
        buffer.readBytes(bytes)
        return bytes
    }
}
