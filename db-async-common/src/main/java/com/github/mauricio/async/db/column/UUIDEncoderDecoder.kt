
package com.github.mauricio.async.db.column

import java.util.UUID

object UUIDEncoderDecoder : ColumnEncoderDecoder {

  override fun decode(value: String): UUID = UUID.fromString(value)

}