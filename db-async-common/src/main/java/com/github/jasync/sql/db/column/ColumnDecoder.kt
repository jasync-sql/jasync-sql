
package com.github.jasync.sql.db.column

import java.nio.charset.Charset
import io.netty.buffer.ByteBuf
import com.github.jasync.sql.db.general.ColumnData

interface ColumnDecoder {

  fun decode( kind : ColumnData, value : ByteBuf, charset : Charset ) : Any? {
    val bytes = ByteArray(value.readableBytes())
    value.readBytes(bytes)
    return decode(String(bytes, charset))
  }

  fun decode( value : String ) : Any?

  fun supportsStringDecoding (): Boolean = true

}
