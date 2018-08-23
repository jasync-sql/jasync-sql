
package com.github.mauricio.async.db.column

object LongEncoderDecoder : ColumnEncoderDecoder {
  override fun decode(value: String): Long = value.toLong()
}
