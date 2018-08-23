
package com.github.mauricio.async.db.column

object FloatEncoderDecoder : ColumnEncoderDecoder {
  override fun decode(value: String): Float = value.toFloat()
}
