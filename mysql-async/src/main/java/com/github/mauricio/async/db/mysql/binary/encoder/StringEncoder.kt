package com.github.mauricio.async.db.mysql.binary.encoder

import com.github.jasync.sql.db.util.writeLenghtEncodedString
import com.github.mauricio.async.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset


class StringEncoder(val charset: Charset) : BinaryEncoder {

  override fun encode(value: Any, buffer: ByteBuf) {
    buffer.writeLenghtEncodedString(value.toString(), charset)
  }

  override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_VARCHAR

}
