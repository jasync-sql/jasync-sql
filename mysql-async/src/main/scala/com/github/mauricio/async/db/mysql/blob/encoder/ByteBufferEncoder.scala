package com.github.mauricio.async.db.mysql.blob.encoder

import java.nio.ByteBuffer

import io.netty.buffer.{ByteBuf, Unpooled}

object ByteBufferEncoder extends BlobEncoder {

  def isLong(value: Any): Boolean = value.asInstanceOf[ByteBuffer].remaining() > BlobEncoder.LONG_THRESHOLD

  def encode(value: Any): ByteBuf = Unpooled.wrappedBuffer(value.asInstanceOf[ByteBuffer])

}
