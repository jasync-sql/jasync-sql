
package com.github.mauricio.async.db.mysql.binary.decoder

import java.nio.charset.Charset
import io.netty.buffer.ByteBuf
import java.math.BigDecimal
import com.github.jasync.sql.db.util.readLengthEncodedString

class BigDecimalDecoder(val charset : Charset ) : BinaryDecoder {
  override fun decode(buffer: ByteBuf): Any {
    return BigDecimal( buffer.readLengthEncodedString(charset) )
  }
}
