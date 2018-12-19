package com.github.jasync.sql.db.column

import sun.net.util.IPAddressUtil.textToNumericFormatV4
import sun.net.util.IPAddressUtil.textToNumericFormatV6
import java.net.InetAddress

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
