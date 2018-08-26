
package com.github.mauricio.async.db.mysql.binary.encoder

import com.github.mauricio.async.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf

object ByteEncoder : BinaryEncoder {

  override fun encode(value: Any, buffer: ByteBuf) {
    buffer.writeByte((value as Byte).toInt())
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_TINY
}
