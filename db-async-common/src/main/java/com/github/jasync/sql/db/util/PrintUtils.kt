package com.github.jasync.sql.db.util

import io.netty.buffer.ByteBuf
import mu.KotlinLogging

object PrintUtils {

    fun printArray(name: String, buffer: ByteBuf) {
        buffer.markReaderIndex()
        val bytes = ByteArray(buffer.readableBytes())
        buffer.readBytes(bytes)
        buffer.resetReaderIndex()
        logger.debug("$name ByteArray(${bytes.joinToString(", ")})")
    }

}

private val logger = KotlinLogging.logger {}
