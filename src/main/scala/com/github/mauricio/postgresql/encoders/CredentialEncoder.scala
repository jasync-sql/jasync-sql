package com.github.mauricio.postgresql.encoders

import com.github.mauricio.postgresql.messages.frontend.{CredentialMessage, FrontendMessage}
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.messages.backend.{Message, AuthenticationResponseType}
import com.github.mauricio.postgresql.util.PostgreSQLMD5Digest
import com.github.mauricio.postgresql.ChannelUtils
import java.nio.charset.Charset

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 6:48 PM
 */
class CredentialEncoder( charset : Charset ) extends Encoder {

  def encode(message: FrontendMessage): ChannelBuffer = {

    val credentialMessage = message.asInstanceOf[CredentialMessage]

    val password = credentialMessage.authenticationType match {
      case AuthenticationResponseType.Cleartext => {
        credentialMessage.password.getBytes(charset)
      }
      case AuthenticationResponseType.MD5 => {
        PostgreSQLMD5Digest.encode(
          credentialMessage.username,
          credentialMessage.password,
          credentialMessage.salt.get,
          charset )
      }
    }

    val buffer = ChannelBuffers.dynamicBuffer(1 + 4 + password.size + 1)
    buffer.writeByte(Message.PasswordMessage)
    buffer.writeInt(0)
    buffer.writeBytes(password)
    buffer.writeByte(0)

    ChannelUtils.writeLength(buffer)

    buffer
  }

}
