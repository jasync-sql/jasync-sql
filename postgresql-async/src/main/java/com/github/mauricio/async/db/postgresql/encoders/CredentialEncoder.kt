package com.github.mauricio.async.db.postgresql.encoders

import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.mauricio.async.db.postgresql.messages.backend.AuthenticationResponseType
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import com.github.mauricio.async.db.postgresql.messages.frontend.ClientMessage
import com.github.mauricio.async.db.postgresql.messages.frontend.CredentialMessage
import com.github.mauricio.async.db.postgresql.util.PasswordHelper
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.charset.Charset


class CredentialEncoder(val charset: Charset) : Encoder {

  override fun encode(message: ClientMessage): ByteBuf {

    val credentialMessage = message as CredentialMessage

    val password = when (credentialMessage.authenticationType) {
      AuthenticationResponseType.Cleartext -> {
        credentialMessage.password.getBytes(charset)
      }
      AuthenticationResponseType.MD5 -> {
        PasswordHelper.encode(
            credentialMessage.username,
            credentialMessage.password,
            credentialMessage.salt,
            charset)
      }
    }

    val buffer = Unpooled.buffer(1 + 4 + password.size + 1)
    buffer.writeByte(ServerMessage.PasswordMessage)
    buffer.writeInt(0)
    buffer.writeBytes(password)
    buffer.writeByte(0)

    ByteBufferUtils.writeLength(buffer)

    return buffer
  }

}
