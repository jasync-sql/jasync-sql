package com.github.jasync.sql.db.mysql.binary.decoder

import com.github.jasync.sql.db.util.readLengthEncodedString
import io.netty.buffer.ByteBuf
import java.math.BigDecimal
import java.nio.charset.Charset

class BigDecimalDecoder(val charset: Charset) : BinaryDecoder {
    override fun decode(buffer: ByteBuf): Any {
        return BigDecimal(buffer.readLengthEncodedString(charset))
    }
}
