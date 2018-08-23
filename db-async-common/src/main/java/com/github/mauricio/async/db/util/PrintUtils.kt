
package com.github.mauricio.async.db.util

import io.netty.buffer.ByteBuf

object PrintUtils {

  private val log = Log.getByName(this.javaClass.name)

  fun printArray( name : String, buffer : ByteBuf ) {
    buffer.markReaderIndex()
    val bytes = ByteArray(buffer.readableBytes())
    buffer.readBytes(bytes)
    buffer.resetReaderIndex()
    log.debug( "$name ByteArray(${bytes.joinToString(", ")})" )
  }

}
