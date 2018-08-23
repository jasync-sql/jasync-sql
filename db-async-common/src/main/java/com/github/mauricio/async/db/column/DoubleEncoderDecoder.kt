
package com.github.mauricio.async.db.column


object DoubleEncoderDecoder : ColumnEncoderDecoder {
  override fun decode(value: String): Double = value.toDouble()
}
