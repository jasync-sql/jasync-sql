package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.util.readCString
import com.github.jasync.sql.db.util.readUntilEOF
import com.github.jasync.sql.db.mysql.message.server.AuthenticationSwitchRequest
import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class AuthenticationSwitchRequestDecoder(val  charset : Charset ) : MessageDecoder {
  override fun decode(buffer: ByteBuf): ServerMessage {
    return AuthenticationSwitchRequest(
      buffer.readCString(charset),
      buffer.readUntilEOF(charset)
    )
  }
}
