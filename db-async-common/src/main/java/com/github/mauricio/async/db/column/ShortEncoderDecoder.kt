
package com.github.mauricio.async.db.column

object ShortEncoderDecoder : ColumnEncoderDecoder {

  override fun decode(value: String): Any = value.toShort()

}
