
package com.github.mauricio.async.db.mysql.binary

import com.github.jasync.sql.db.exceptions.BufferNotFullyConsumedException
import com.github.mauricio.async.db.mysql.message.server.ColumnDefinitionMessage
import io.netty.buffer.ByteBuf
import kotlin.experimental.and

private val BitMapOffset = 9

class BinaryRowDecoder {

  //import BinaryRowDecoder._

  fun decode(buffer: ByteBuf, columns: List<ColumnDefinitionMessage>): Array<Any?> {

    //log.debug("columns are {} - {}", buffer.readableBytes(), columns)
    //log.debug( "decoding row\n{}", MySQLHelper.dumpAsHex(buffer))
    //PrintUtils.printArray("bitmap", buffer)

    val nullCount = (columns.size + 9) / 8

    val nullBitMask = ByteArray(nullCount)
    buffer.readBytes(nullBitMask)

    var nullMaskPos = 0
    //TODO is this really int?
    var bit: Int = 4

    val row = mutableListOf<Any?>()

    var index = 0

    while (index < columns.size) {

      if ((nullBitMask[nullMaskPos].toInt() and bit) != 0) {
        row.add(null)
      } else {

        val column = columns[index]

        //log.debug(s"${decoder.getClass.getSimpleName} - ${buffer.readableBytes()}")
        //log.debug("Column value <{}> - {}", value, column.name)

        row += column.binaryDecoder.decode(buffer)
      }

      bit = bit shl 1

      if (( bit and 255) == 0) {
        bit = 1
        nullMaskPos += 1
      }

      index += 1
    }

    //log.debug("values are {}", row)

    if (buffer.readableBytes() != 0) {
      throw BufferNotFullyConsumedException(buffer)
    }

    return row.toTypedArray()
  }

}


