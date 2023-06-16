package com.github.jasync.sql.db.mysql.binary

import com.github.jasync.sql.db.exceptions.BufferNotFullyConsumedException
import com.github.jasync.sql.db.mysql.message.server.ColumnDefinitionMessage
import com.github.jasync.sql.db.util.BufferDumper
import com.github.jasync.sql.db.util.PrintUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import io.netty.buffer.ByteBuf

private val logger = KotlinLogging.logger {}

class BinaryRowDecoder {

    fun decode(buffer: ByteBuf, columns: List<ColumnDefinitionMessage>): Array<Any?> {
        logger.trace { "columns are ${buffer.readableBytes()} - $columns" }
        logger.trace { "decoding row\n${BufferDumper.dumpAsHex(buffer)}" }
        PrintUtils.printArray("bitmap", buffer)

        val nullCount = (columns.size + 9) / 8

        val nullBitMask = ByteArray(nullCount)
        buffer.readBytes(nullBitMask)

        var nullMaskPos = 0

        @Suppress("RedundantExplicitType")
        var bit: Int = 4

        val row = Array<Any?>(columns.size) {
            val result = if ((nullBitMask[nullMaskPos].toInt() and bit) != 0) {
                null
            } else {
                val column = columns[it]

                logger.trace { "${buffer.readableBytes()}" }
                logger.trace { "Column ${column.name}" }

                column.binaryDecoder.decode(buffer)
            }

            bit = bit shl 1

            if ((bit and 255) == 0) {
                bit = 1
                nullMaskPos += 1
            }
            result
        }

        logger.trace { "values are $row" }

        if (buffer.readableBytes() != 0) {
            throw BufferNotFullyConsumedException(buffer)
        }

        return row
    }
}
