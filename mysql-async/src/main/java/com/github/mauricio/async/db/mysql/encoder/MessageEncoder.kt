
package com.github.mauricio.async.db.mysql.encoder

import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.message.client.ClientMessage

interface MessageEncoder {

  fun encode( message : ClientMessage ) : ByteBuf

}