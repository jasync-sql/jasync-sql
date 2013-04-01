package com.github.mauricio.postgresql.encoders

import com.github.mauricio.postgresql.messages.frontend.{CredentialMessage, FrontendMessage}
import org.jboss.netty.buffer.{ChannelBuffers, ChannelBuffer}
import com.github.mauricio.postgresql.messages.backend.{Message, AuthenticationResponseType}
import org.jboss.netty.util.CharsetUtil
import com.github.mauricio.postgresql.util.PostgreSQLMD5Digest
import com.github.mauricio.postgresql.ChannelUtils

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 6:48 PM
 */
object CredentialEncoder extends Encoder {

  def encode(message: FrontendMessage): ChannelBuffer = {

    val credentialMessage = message.asInstanceOf[CredentialMessage]

    val password = credentialMessage.authenticationType match {
      case AuthenticationResponseType.Cleartext => {
        credentialMessage.password.getBytes(CharsetUtil.UTF_8)
      }
      case AuthenticationResponseType.MD5 => {
        PostgreSQLMD5Digest.encode( credentialMessage.username, credentialMessage.password, credentialMessage.salt.get )
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
