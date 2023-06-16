package com.github.jasync.sql.db.mysql.binary.decoder

import io.netty.buffer.ByteBuf
import mu.KotlinLogging
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

object TimestampDecoder : BinaryDecoder {
    override fun decode(buffer: ByteBuf): LocalDateTime? {
        val size = buffer.readUnsignedByte()

        return when (size) {
            0.toShort() -> null
            4.toShort() -> LocalDateTime.of(
                buffer.readUnsignedShort(),
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
                0,
                0,
                0,
                0
            )
            7.toShort() -> LocalDateTime.of(
                buffer.readUnsignedShort(),
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
                0
            )
            11.toShort() -> LocalDateTime.of(
                buffer.readUnsignedShort(),
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedByte().toInt(),
                buffer.readUnsignedInt().toInt() * 1000
            )
            // millis = x / 1000
            // nanos = millis * 1000000
            // x * 1000
            else -> {
                logger.warn { "unknown decoded size $size" }
                null
            }
        }
    }
}
