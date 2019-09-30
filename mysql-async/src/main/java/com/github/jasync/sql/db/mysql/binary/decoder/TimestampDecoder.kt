package com.github.jasync.sql.db.mysql.binary.decoder

import io.netty.buffer.ByteBuf
import mu.KotlinLogging
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

private val logger = KotlinLogging.logger {}

object TimestampDecoder : BinaryDecoder {
    override fun decode(buffer: ByteBuf): LocalDateTime? {
        val size = buffer.readUnsignedByte()

        return when (size) {
            0.toShort() -> null
            4.toShort() -> LocalDateTime.of(
                    LocalDate.of(
                            buffer.readUnsignedShort(),
                            buffer.readUnsignedByte().toInt(),
                            buffer.readUnsignedByte().toInt()
                    ),
                    LocalTime.of(
                            0,
                            0,
                            0,
                            0
                    )
            )
            7.toShort() -> LocalDateTime.of(
                    LocalDate.of(
                            buffer.readUnsignedShort(),
                            buffer.readUnsignedByte().toInt(),
                            buffer.readUnsignedByte().toInt()
                    ),
                    LocalTime.of(
                            buffer.readUnsignedByte().toInt(),
                            buffer.readUnsignedByte().toInt(),
                            buffer.readUnsignedByte().toInt(),
                            0
                    )
            )
            11.toShort() -> LocalDateTime.of(
                    LocalDate.of(
                            buffer.readUnsignedShort(),
                            buffer.readUnsignedByte().toInt(),
                            buffer.readUnsignedByte().toInt()
                    ),
                    LocalTime.of(
                            buffer.readUnsignedByte().toInt(),
                            buffer.readUnsignedByte().toInt(),
                            buffer.readUnsignedByte().toInt(),
                            buffer.readUnsignedByte().toInt() * 1_000_000
                    )
            )
            else -> {
                logger.warn { "unknown decoded size $size" }
                null
            }
        }
    }
}
