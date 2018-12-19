package com.github.jasync.sql.db.mysql.binary.decoder

import com.github.jasync.sql.db.util.readLengthEncodedString
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset


class StringDecoder(val charset: Charset) : BinaryDecoder {

    override fun decode(buffer: ByteBuf): Any {
        return buffer.readLengthEncodedString(charset)
    }
}
