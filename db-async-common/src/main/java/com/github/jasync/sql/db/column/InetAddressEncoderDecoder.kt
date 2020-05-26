package com.github.jasync.sql.db.column

import java.net.InetAddress
import sun.net.util.IPAddressUtil.textToNumericFormatV4
import sun.net.util.IPAddressUtil.textToNumericFormatV6

object InetAddressEncoderDecoder : ColumnEncoderDecoder {

    override fun decode(value: String): Any {
        return if (value.contains(':')) {
            InetAddress.getByAddress(textToNumericFormatV6(value))
        } else {
            InetAddress.getByAddress(textToNumericFormatV4(value))
        }
    }

    override fun encode(value: Any): String {
        return (value as InetAddress).hostAddress
    }
}
