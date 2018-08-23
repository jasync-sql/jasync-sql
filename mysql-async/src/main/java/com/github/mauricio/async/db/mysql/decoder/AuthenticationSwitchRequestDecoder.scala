package com.github.mauricio.async.db.mysql.decoder

import java.nio.charset.Charset
import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.message.server.{AuthenticationSwitchRequest, ServerMessage}
import com.github.mauricio.async.db.util.ChannelWrapper.bufferToWrapper

class AuthenticationSwitchRequestDecoder( charset : Charset ) extends MessageDecoder {
  def decode(buffer: ByteBuf): ServerMessage = {
    new AuthenticationSwitchRequest(
      buffer.readCString(charset),
      buffer.readUntilEOF(charset)
    )
  }
}
