
package com.github.jasync.sql.db.column

object ByteDecoder : ColumnDecoder {
  override fun decode(value: String): Any = value.toByte()
}
