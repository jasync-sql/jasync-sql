
package com.github.jasync.sql.db.column

import java.math.BigInteger

object BigIntegerEncoderDecoder : ColumnEncoderDecoder {
  override fun decode(value: String): Any = BigInteger(value)
}
