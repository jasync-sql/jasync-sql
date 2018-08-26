
package com.github.mauricio.async.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.column.ColumnTypes

object LongEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    buffer.writeLong(value as Long)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_LONGLONG
}
