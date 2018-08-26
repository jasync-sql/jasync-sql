
package com.github.mauricio.async.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.column.ColumnTypes

object DoubleEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    buffer.writeDouble(value as Double)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_DOUBLE
}
