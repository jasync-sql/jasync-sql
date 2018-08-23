
package com.github.jasync.sql.db.column


object DoubleEncoderDecoder : ColumnEncoderDecoder {
  override fun decode(value: String): Double = value.toDouble()
}
