package com.github.jasync.sql.db.column

import java.math.BigDecimal

object BigDecimalEncoderDecoder : ColumnEncoderDecoder {

    override fun decode(value: String): Any = BigDecimal(value)

}
