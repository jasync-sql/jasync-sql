package com.github.jasync.sql.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf

interface BinaryEncoder {

    fun encode(value: Any, buffer: ByteBuf)

    fun encodesTo(): Int

}
