package com.github.mauricio.async.db.postgresql.encoders

import com.github.mauricio.async.db.postgresql.messages.frontend.ClientMessage
import io.netty.buffer.ByteBuf

interface Encoder {
  fun encode(message: ClientMessage): ByteBuf
}
