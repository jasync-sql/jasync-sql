package com.github.jasync.sql.db.mysql.encoder

import com.github.jasync.sql.db.mysql.binary.BinaryRowEncoder
import com.github.jasync.sql.db.mysql.column.ColumnTypes
import com.github.jasync.sql.db.mysql.message.client.ClientMessage
import com.github.jasync.sql.db.mysql.message.client.PreparedStatementExecuteMessage
import com.github.jasync.sql.db.util.ByteBufferUtils
import com.github.jasync.sql.db.util.length
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import kotlin.experimental.or

class PreparedStatementExecuteEncoder(private val rowEncoder: BinaryRowEncoder) : MessageEncoder {

    override fun encode(message: ClientMessage): ByteBuf {
        val m = message as PreparedStatementExecuteMessage

        val buffer = ByteBufferUtils.packetBuffer()
        buffer.writeByte(m.kind)
        buffer.writeBytes(m.statementId)
        buffer.writeByte(0x00) // no cursor
        buffer.writeInt(1)

        return if (m.parameters.isEmpty()) {
            buffer
        } else {
            Unpooled.wrappedBuffer(buffer, encodeValues(m.values, m.valuesToInclude))
        }

    }

    fun encodeValues(values: List<Any?>, valuesToInclude: Set<Int>): ByteBuf {
        val nullBitsCount = (values.size + 7) / 8
        val nullBits = ByteArray(nullBitsCount)
        val bitMapBuffer = ByteBufferUtils.mysqlBuffer(1 + nullBitsCount)
        val parameterTypesBuffer = ByteBufferUtils.mysqlBuffer(values.size * 2)
        val parameterValuesBuffer = ByteBufferUtils.mysqlBuffer()

        var index = 0

        while (index < values.length) {
            val value = values[index]
            if (value == null) {
                nullBits[index / 8] = (nullBits[index / 8] or (1 shl (index and 7)).toByte())
                parameterTypesBuffer.writeShort(ColumnTypes.FIELD_TYPE_NULL)
            } else {
                encodeValue(parameterTypesBuffer, parameterValuesBuffer, value, valuesToInclude.contains(index))
            }
            index += 1
        }

        bitMapBuffer.writeBytes(nullBits)
        if (values.isNotEmpty()) {
            bitMapBuffer.writeByte(1)
        } else {
            bitMapBuffer.writeByte(0)
        }

        return Unpooled.wrappedBuffer(bitMapBuffer, parameterTypesBuffer, parameterValuesBuffer)
    }

    fun encodeValue(parameterTypesBuffer: ByteBuf, parameterValuesBuffer: ByteBuf, value: Any, includeValue: Boolean) {
        val encoder = rowEncoder.encoderFor(value)
        parameterTypesBuffer.writeShort(encoder.encodesTo())
        if (includeValue)
            encoder.encode(value, parameterValuesBuffer)
    }

}
