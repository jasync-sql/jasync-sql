package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.exceptions.UnsupportedAuthenticationMethodException
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationCleartextPasswordMessage
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationMD5PasswordMessage
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationOkMessage
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationSASLContinueMessage
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationSASLFinalMessage
import com.github.jasync.sql.db.postgresql.messages.backend.AuthenticationSASLMessage
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
    const val AuthenticationSASL = 10
    const val AuthenticationSASLContinue = 11
    const val AuthenticationSASLFinal = 12

    override fun parseMessage(buffer: ByteBuf): ServerMessage {
        val authenticationType = buffer.readInt()
        val bytes = ByteArray(buffer.readableBytes()).also { buffer.readBytes(it) }

        return when (authenticationType) {
            AuthenticationOk -> AuthenticationOkMessage
            AuthenticationCleartextPassword -> AuthenticationCleartextPasswordMessage
            AuthenticationMD5Password -> AuthenticationMD5PasswordMessage(bytes)
            AuthenticationSASL -> AuthenticationSASLMessage(parseSASLMechanismIds(bytes))
            AuthenticationSASLContinue -> AuthenticationSASLContinueMessage(bytes.decodeToString())
            AuthenticationSASLFinal -> AuthenticationSASLFinalMessage(bytes.decodeToString())
            else -> throw UnsupportedAuthenticationMethodException(authenticationType)
        }
    }

    fun parseSASLMechanismIds(bytes: ByteArray): List<String> {
        val supportedMechanismStrings = mutableListOf<String>()
        val builder = StringBuilder()

        bytes.forEach { byte ->
            if (byte != 0.toByte()) {
                builder.append(byte.toChar())
            } else {
                builder.takeIf { it.isNotEmpty() }?.let { supportedMechanismStrings.add(it.toString()) }
                builder.clear()
            }
        }

        return supportedMechanismStrings
    }
}
