package com.github.jasync.sql.db.column

import java.net.InetAddress

object InetAddressEncoderDecoder : ColumnEncoderDecoder {
    override fun decode(value: String): Any = InetAddress.getByName(value)
    override fun encode(value: Any): String = (value as InetAddress).hostAddress
}
