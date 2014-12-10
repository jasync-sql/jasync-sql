package com.github.mauricio.async.db.mysql.blob.encoder

import io.netty.buffer.{ByteBuf, Unpooled}

object ByteArrayEncoder extends BlobEncoder {

  def isLong(value: Any): Boolean = value.asInstanceOf[Array[Byte]].length > BlobEncoder.LONG_THRESHOLD

  def encode(value: Any): ByteBuf = Unpooled.wrappedBuffer(value.asInstanceOf[Array[Byte]])

}
