package com.github.mauricio.async.db.postgresql.parsers

import com.github.jasync.sql.db.exceptions.UnsupportedAuthenticationMethodException
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf


object AuthenticationStartupParser : MessageParser {

  val AuthenticationOk = 0
  val AuthenticationKerberosV5 = 2
  val AuthenticationCleartextPassword = 3
  val AuthenticationMD5Password = 5
  val AuthenticationSCMCredential = 6
  val AuthenticationGSS = 7
  val AuthenticationGSSContinue = 8
  val AuthenticationSSPI = 9

  override fun parseMessage(b: ByteBuf): ServerMessage {
    val authenticationType = b.readInt()
    when (authenticationType) {

      case AuthenticationOk => AuthenticationOkMessage . Instance
          case AuthenticationCleartextPassword => AuthenticationChallengeCleartextMessage . Instance
          case AuthenticationMD5Password => {
        val bytes = new Array [Byte](b.readableBytes())
        b.readBytes(bytes)
        new AuthenticationChallengeMD5 (bytes)
      }
          case _ => {
        throw  UnsupportedAuthenticationMethodException(authenticationType)
      }

    }

  }

}