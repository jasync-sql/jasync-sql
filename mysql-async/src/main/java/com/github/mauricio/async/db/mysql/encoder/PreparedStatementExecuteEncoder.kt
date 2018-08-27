
package com.github.mauricio.async.db.mysql.encoder

import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.jasync.sql.db.util.length
import com.github.mauricio.async.db.mysql.binary.BinaryRowEncoder
import com.github.mauricio.async.db.mysql.column.ColumnTypes
import com.github.mauricio.async.db.mysql.message.client.ClientMessage
import com.github.mauricio.async.db.mysql.message.client.PreparedStatementExecuteMessage
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import sun.java2d.xr.XRUtils.None
import kotlin.experimental.or

class PreparedStatementExecuteEncoder( val rowEncoder : BinaryRowEncoder ) : MessageEncoder {

  override fun encode(message: ClientMessage): ByteBuf {
    val m = message as PreparedStatementExecuteMessage

    val buffer = ByteBufferUtils.packetBuffer()
    buffer.writeByte( m.kind )
    buffer.writeBytes(m.statementId)
    buffer.writeByte(0x00) // no cursor
    buffer.writeInt(1)

    return if ( m.parameters.isEmpty() ) {
      buffer
    } else {
      Unpooled.wrappedBuffer(buffer, encodeValues(m.values, m.valuesToInclude))
    }

  }

  private fun encodeValues( values : List<Any?>, valuesToInclude: Set<Int> ) : ByteBuf {
    val nullBitsCount = (values.size + 7) / 8
    val nullBits = ByteArray(nullBitsCount)
    val bitMapBuffer = ByteBufferUtils.mysqlBuffer(1 + nullBitsCount)
    val parameterTypesBuffer = ByteBufferUtils.mysqlBuffer(values.size * 2)
    val parameterValuesBuffer = ByteBufferUtils.mysqlBuffer()

    var index = 0

    while ( index < values.length ) {
      val value = values[index]
      if ( value == null || value == None ) {
        nullBits[index / 8] = (nullBits[index / 8] or (1 shl (index and 7)).toByte())
        parameterTypesBuffer.writeShort(ColumnTypes.FIELD_TYPE_NULL)
      } else {
        encodeValue(parameterTypesBuffer, parameterValuesBuffer, value, valuesToInclude.contains(index))
      }
      index += 1
    }

    bitMapBuffer.writeBytes(nullBits)
    if ( values.size > 0 ) {
      bitMapBuffer.writeByte(1)
    } else {
      bitMapBuffer.writeByte(0)
    }

    return Unpooled.wrappedBuffer( bitMapBuffer, parameterTypesBuffer, parameterValuesBuffer )
  }

  private fun encodeValue(parameterTypesBuffer: ByteBuf, parameterValuesBuffer: ByteBuf, value: Any, includeValue: Boolean) : Unit {
    val encoder = rowEncoder.encoderFor(value)
    parameterTypesBuffer.writeShort(encoder.encodesTo())
    if (includeValue)
      encoder.encode(value, parameterValuesBuffer)
  }

}
