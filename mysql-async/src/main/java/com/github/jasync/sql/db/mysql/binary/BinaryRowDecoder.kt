package com.github.jasync.sql.db.mysql.binary

import com.github.jasync.sql.db.exceptions.BufferNotFullyConsumedException
import com.github.jasync.sql.db.mysql.message.server.ColumnDefinitionMessage
import io.netty.buffer.ByteBuf
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

private val BitMapOffset = 9

class BinaryRowDecoder {

    fun decode(buffer: ByteBuf, columns: List<ColumnDefinitionMessage>): Array<Any?> {

        //log.debug("columns are {} - {}", buffer.readableBytes(), columns)
        //log.debug( "decoding row\n{}", MySQLHelper.dumpAsHex(buffer))
        //PrintUtils.printArray("bitmap", buffer)

        val nullCount = (columns.size + 9) / 8

        val nullBitMask = ByteArray(nullCount)
        buffer.readBytes(nullBitMask)

        var nullMaskPos = 0

        var bit: Int = 4

        val row = Array<Any?>(columns.size) {
            val result = if ((nullBitMask[nullMaskPos].toInt() and bit) != 0) {
                null
            } else {

                val column = columns[it]

                //log.debug(s"${decoder.getClass.getSimpleName} - ${buffer.readableBytes()}")
                //log.debug("Column value <{}> - {}", value, column.name)

                column.binaryDecoder.decode(buffer)
            }

            bit = bit shl 1

            if ((bit and 255) == 0) {
                bit = 1
                nullMaskPos += 1
            }
            result
        }


        //log.debug("values are {}", row)

        if (buffer.readableBytes() != 0) {
            throw BufferNotFullyConsumedException(buffer)
        }

        return row
    }

}


