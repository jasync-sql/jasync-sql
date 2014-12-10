package com.github.mauricio.async.db.mysql.binary.encoder

import com.github.mauricio.async.db.mysql.column.ColumnTypes
import com.github.mauricio.async.db.util.ChannelWrapper.bufferToWrapper
import io.netty.buffer.ByteBuf

object ByteBufEncoder extends BinaryEncoder {
  def encode(value: Any, buffer: ByteBuf) {
    val bytes = value.asInstanceOf[ByteBuf]

    buffer.writeLength(bytes.readableBytes())
    buffer.writeBytes(bytes)
  }

  def encodesTo: Int = ColumnTypes.FIELD_TYPE_BLOB

}
