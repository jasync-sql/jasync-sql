package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.mysql.encoder.auth.AuthenticationMethod
import com.github.jasync.sql.db.mysql.message.server.HandshakeMessage
import com.github.jasync.sql.db.mysql.message.server.ServerMessage
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_PLUGIN_AUTH
import com.github.jasync.sql.db.mysql.util.MySQLIO.CLIENT_SECURE_CONNECTION
import com.github.jasync.sql.db.util.readCString
import com.github.jasync.sql.db.util.readUntilEOF
import io.netty.buffer.ByteBuf
import io.netty.util.CharsetUtil
import kotlin.experimental.and
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class HandshakeV10Decoder : MessageDecoder {

    companion object {
        private const val SeedSize = 8
        private const val SeedComplementSize = 12
        private const val Padding = 10
        private val ASCII = CharsetUtil.US_ASCII
    }

    override fun decode(buffer: ByteBuf): ServerMessage {

        val serverVersion = buffer.readCString(ASCII)
        val connectionId = buffer.readUnsignedInt()

        val seed = ByteArray(SeedSize + SeedComplementSize)
        buffer.readBytes(seed, 0, SeedSize)

        buffer.readByte() // filler

        // read capability flags (lower 2 bytes)
        var serverCapabilityFlags = buffer.readUnsignedShort()

        /* New protocol , 16 bytes to describe server characteristics */
        // read character set (1 byte)
        val characterSet = buffer.readByte() and 0xff.toByte()
        // read status flags (2 bytes)
        val statusFlags = buffer.readUnsignedShort()

        // read capability flags (upper 2 bytes)
        serverCapabilityFlags = serverCapabilityFlags or (buffer.readUnsignedShort() shl 16)

        var authPluginDataLength = 0.toByte()
        var authenticationMethod = AuthenticationMethod.Native

        if ((serverCapabilityFlags and CLIENT_PLUGIN_AUTH) != 0) {
            // read length of auth-plugin-data (1 byte)
            authPluginDataLength = buffer.readByte() and 0xff.toByte()
        } else {
            // read filler (<00>)
            buffer.readByte()
        }

        // next 10 bytes are reserved (all <00>)
        buffer.readerIndex(buffer.readerIndex() + Padding)

        logger.debug("Auth plugin data length was $authPluginDataLength")

        if ((serverCapabilityFlags and CLIENT_SECURE_CONNECTION) != 0) {
            val complement = if (authPluginDataLength > 0) {
                authPluginDataLength - 1 - SeedSize
            } else {
                SeedComplementSize
            }

            buffer.readBytes(seed, SeedSize, complement)
            buffer.readByte()
        }

        if ((serverCapabilityFlags and CLIENT_PLUGIN_AUTH) != 0) {
            authenticationMethod = buffer.readUntilEOF(ASCII)
        }

        val message = HandshakeMessage(
            serverVersion,
            connectionId,
            seed,
            serverCapabilityFlags,
            characterSet = characterSet.toInt(),
            statusFlags = statusFlags,
            authenticationMethod = authenticationMethod
        )

        logger.debug { "handshake message was $message" }

        return message
    }
}
