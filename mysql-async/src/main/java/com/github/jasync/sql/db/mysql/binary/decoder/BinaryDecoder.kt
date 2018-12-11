package com.github.jasync.sql.db.mysql.binary.decoder

import io.netty.buffer.ByteBuf

interface BinaryDecoder {

    fun decode(buffer: ByteBuf): Any?

}
