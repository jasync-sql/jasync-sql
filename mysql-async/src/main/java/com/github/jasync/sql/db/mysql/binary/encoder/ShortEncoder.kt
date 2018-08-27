
package com.github.jasync.sql.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf
import com.github.jasync.sql.db.mysql.column.ColumnTypes

object ShortEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    buffer.writeShort((value as Short).toInt())
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_SHORT
}
