
package com.github.mauricio.async.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.column.ColumnTypes

object IntegerEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    buffer.writeInt(value as Int)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_LONG
}
