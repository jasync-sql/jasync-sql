
package com.github.mauricio.async.db.mysql.binary.encoder

import io.netty.buffer.ByteBuf

interface BinaryEncoder {

  override fun encode( value : Any, buffer : ByteBuf )

  fun encodesTo (): Int

}
