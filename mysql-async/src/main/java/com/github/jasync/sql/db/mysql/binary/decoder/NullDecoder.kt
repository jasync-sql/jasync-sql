package com.github.jasync.sql.db.mysql.binary.decoder

import io.netty.buffer.ByteBuf

object NullDecoder : BinaryDecoder {
    override fun decode(buffer: ByteBuf): Any? = null
}
