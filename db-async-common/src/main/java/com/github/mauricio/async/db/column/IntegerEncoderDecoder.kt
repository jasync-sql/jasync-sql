
package com.github.mauricio.async.db.column

object IntegerEncoderDecoder : ColumnEncoderDecoder {

  override fun decode(value: String): Int = value.toInt()

}
