
package com.github.mauricio.async.db.column

import java.nio.charset.Charset
import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.general.ColumnData

interface ColumnDecoderRegistry {

  fun decode(kind: ColumnData, value: ByteBuf, charset : Charset) : Any

}