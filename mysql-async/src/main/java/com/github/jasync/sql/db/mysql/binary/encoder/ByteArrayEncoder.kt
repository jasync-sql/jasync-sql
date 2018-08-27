

package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.util.writeLength
import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf

object ByteArrayEncoder : BinaryEncoder {
  override fun encode(value: Any, buffer: ByteBuf) {
    val bytes = value as ByteArray

    buffer.writeLength(bytes.size.toLong())
    buffer.writeBytes(bytes)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_BLOB

}
