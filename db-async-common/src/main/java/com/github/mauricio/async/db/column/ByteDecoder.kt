
package com.github.mauricio.async.db.column

object ByteDecoder : ColumnDecoder {
  override fun decode(value: String): Any = value.toByte()
}
