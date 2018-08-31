package com.github.mauricio.async.db.postgresql.parsers

import com.github.jasync.sql.db.util.length
import com.github.mauricio.async.db.postgresql.messages.backend.DataRowMessage
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf


object DataRowParser : MessageParser {

  override fun parseMessage(buffer: ByteBuf): ServerMessage {
    val row = mutableListOf<ByteBuf?>()
    0.until(row.length).forEach { column ->
      val length = buffer.readInt()
      row[column] = if (length == -1) {
        null
      } else {
        buffer.readBytes(length)
      }
    }
    return DataRowMessage(row.toTypedArray())
  }

}
