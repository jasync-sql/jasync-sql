package com.github.jasync.sql.db.column

import java.util.*

object UUIDEncoderDecoder : ColumnEncoderDecoder {

    override fun decode(value: String): UUID = UUID.fromString(value)

}
