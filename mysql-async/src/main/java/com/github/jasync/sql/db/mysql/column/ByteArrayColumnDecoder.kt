
package com.github.jasync.sql.db.mysql.column

import com.github.jasync.sql.db.column.ColumnDecoder
import com.github.jasync.sql.db.general.ColumnData
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

object ByteArrayColumnDecoder : ColumnDecoder {

  override fun decode(kind: ColumnData, value: ByteBuf, charset: Charset): Any {
    val bytes = ByteArray(value.readableBytes())
    value.readBytes(bytes)
    return bytes
  }

  override fun decode(value: String): Any {
    throw UnsupportedOperationException("This method should never be called for byte arrays")
  }
}
