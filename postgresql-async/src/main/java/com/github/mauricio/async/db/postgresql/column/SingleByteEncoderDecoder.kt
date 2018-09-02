package com.github.mauricio.async.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnEncoderDecoder
import com.github.mauricio.async.db.column.ColumnEncoderDecoder

object SingleByteEncoderDecoder : ColumnEncoderDecoder {

  override fun encode(value: Any): String {
    val byte = value as Byte
    return ByteArrayEncoderDecoder.encode(Array(byte))
  }

  override fun decode(value: String): Any {
    ByteArrayEncoderDecoder.decode(value)(0)
  }

}