
package com.github.mauricio.async.db.column


object StringEncoderDecoder : ColumnEncoderDecoder {
  override fun decode(value: String): String = value
}