
package com.github.mauricio.async.db.mysql.binary.decoder

import io.netty.buffer.ByteBuf

object ByteDecoder : BinaryDecoder {
  override fun decode(buffer: ByteBuf): Any = buffer.readByte()
}
