package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.exceptions.UnsupportedAuthenticationMethodException
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationChallengeCleartextMessage
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationChallengeMD5
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationOkMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
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
    return when (authenticationType) {
      AuthenticationOk -> AuthenticationOkMessage
      AuthenticationCleartextPassword -> AuthenticationChallengeCleartextMessage
      AuthenticationMD5Password -> {
        val bytes = ByteArray(b.readableBytes())
        b.readBytes(bytes)
        AuthenticationChallengeMD5(bytes)
      }
      else -> {
        throw  UnsupportedAuthenticationMethodException(authenticationType)
      }

    }

  }

}
