
package com.github.jasync.sql.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import com.github.jasync.sql.db.mysql.column.ColumnTypes

object LongEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    buffer.writeLong(value as Long)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_LONGLONG
}
