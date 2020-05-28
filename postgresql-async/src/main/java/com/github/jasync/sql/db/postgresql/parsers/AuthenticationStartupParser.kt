package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.exceptions.UnsupportedAuthenticationMethodException
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationChallengeCleartextMessage
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationChallengeMD5
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationOkMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf

@Suppress("MemberVisibilityCanBePrivate", "unused")
object AuthenticationStartupParser : MessageParser {

    const val AuthenticationOk = 0
    const val AuthenticationKerberosV5 = 2
    const val AuthenticationCleartextPassword = 3
    const val AuthenticationMD5Password = 5
    const val AuthenticationSCMCredential = 6
    const val AuthenticationGSS = 7
    const val AuthenticationGSSContinue = 8
    const val AuthenticationSSPI = 9

    override fun parseMessage(buffer: ByteBuf): ServerMessage {
        val authenticationType = buffer.readInt()
        return when (authenticationType) {
            AuthenticationOk -> AuthenticationOkMessage
            AuthenticationCleartextPassword -> AuthenticationChallengeCleartextMessage
            AuthenticationMD5Password -> {
                val bytes = ByteArray(buffer.readableBytes())
                buffer.readBytes(bytes)
                AuthenticationChallengeMD5(bytes)
            }
            else -> {
                throw UnsupportedAuthenticationMethodException(authenticationType)
            }
        }
    }
}
