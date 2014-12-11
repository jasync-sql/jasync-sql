package com.github.mauricio.async.db.mysql.binary.encoder

import com.github.mauricio.async.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf

object DummyBlobEncoder extends BinaryEncoder {

  def encode(value: Any, buffer: ByteBuf): Unit = {
    throw new UnsupportedOperationException()
  }

  def encodesTo: Int = ColumnTypes.FIELD_TYPE_BLOB

 }
