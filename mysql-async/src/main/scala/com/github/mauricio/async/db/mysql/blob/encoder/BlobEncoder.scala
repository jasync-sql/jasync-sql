package com.github.mauricio.async.db.mysql.blob.encoder

import java.nio.ByteBuffer

import io.netty.buffer.ByteBuf

object BlobEncoder {

  val LONG_THRESHOLD = 1023

  def encoderFor( v : Any ) : Option[BlobEncoder] = {

    v match {
      case v : Array[Byte] => Some(ByteArrayEncoder)
      case v : ByteBuffer => Some(ByteBufferEncoder)
      case v : ByteBuf => Some(ByteBufEncoder)

      case _ => None
    }

  }

}

trait BlobEncoder {

  def isLong(value: Any): Boolean

  def encode(value: Any): ByteBuf

}