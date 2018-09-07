package com.github.jasync.sql.db.postgresql.encoders

import com.github.jasync.sql.db.postgresql.messages.frontend.ClientMessage
import io.netty.buffer.ByteBuf

interface Encoder {
  fun encode(message: ClientMessage): ByteBuf
}
