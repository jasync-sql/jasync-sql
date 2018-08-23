
package com.github.mauricio.async.db.column

import java.nio.charset.Charset
import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.general.ColumnData

interface ColumnDecoder {

  fun decode( kind : ColumnData, value : ByteBuf, charset : Charset ) : Any? {
    val bytes = ByteArray(value.readableBytes())
    value.readBytes(bytes)
    return decode(String(bytes, charset))
  }

  fun decode( value : String ) : Any?

  fun supportsStringDecoding (): Boolean = true

}
