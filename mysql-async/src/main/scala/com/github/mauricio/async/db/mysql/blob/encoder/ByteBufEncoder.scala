package com.github.mauricio.async.db.mysql.blob.encoder

import io.netty.buffer.ByteBuf

object ByteBufEncoder extends BlobEncoder {

  def isLong(value: Any): Boolean = value.asInstanceOf[ByteBuf].readableBytes() > BlobEncoder.LONG_THRESHOLD

  def encode(value: Any): ByteBuf = value.asInstanceOf[ByteBuf]

}
