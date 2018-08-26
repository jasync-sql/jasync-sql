
package com.github.mauricio.async.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.column.ColumnTypes

object FloatEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    buffer.writeFloat(value as Float)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_FLOAT
}
