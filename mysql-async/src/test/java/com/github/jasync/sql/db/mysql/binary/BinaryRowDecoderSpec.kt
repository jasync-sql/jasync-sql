@file:Suppress("DEPRECATION")

package com.github.jasync.sql.db.mysql.binary

import com.github.jasync.sql.db.mysql.codec.DecoderRegistry
import com.github.jasync.sql.db.mysql.column.ColumnTypes
import com.github.jasync.sql.db.mysql.message.server.ColumnDefinitionMessage
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import org.junit.Test
import java.nio.ByteOrder
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BinaryRowDecoderSpec {

    val registry = DecoderRegistry(CharsetUtil.UTF_8)
    val decoder = BinaryRowDecoder()

    val idAndName = byteArrayOf(0, 1, 0, 0, 0, 0, 0, 0, 0, 3, 106, 111, 101)
    val idAndNameColumns = listOf<ColumnDefinitionMessage>(
        createColumn("id", ColumnTypes.FIELD_TYPE_LONGLONG),
        createColumn("name", ColumnTypes.FIELD_TYPE_VAR_STRING)
    )

    val idNameAndNull = byteArrayOf(16, 1, 0, 0, 0, 0, 0, 0, 0, 3, 106, 111, 101)
    val idNameAndNullColumns = listOf<ColumnDefinitionMessage>(
        createColumn("id", ColumnTypes.FIELD_TYPE_LONGLONG),
        createColumn("name", ColumnTypes.FIELD_TYPE_VAR_STRING),
        createColumn("null_value", ColumnTypes.FIELD_TYPE_NULL)
    )

    @Test
    fun `decoder a long and a string from the byte array`() {

        @Suppress("DEPRECATION")
        val buffer = Unpooled.wrappedBuffer(idAndName).order(ByteOrder.LITTLE_ENDIAN)
        val result = decoder.decode(buffer, idAndNameColumns)
        buffer.release()
        assertEquals(1L, result[0])
        assertEquals("joe", result[1])

    }

    @Test
    fun `decode a row with an long, a string and a null`() {
        val buffer = Unpooled.wrappedBuffer(idNameAndNull).order(ByteOrder.LITTLE_ENDIAN)
        val result = decoder.decode(buffer, idNameAndNullColumns)
        buffer.release()
        assertEquals(1L, result[0])
        assertEquals("joe", result[1])
        assertNull(result[2])
    }


    private fun createColumn(name: String, columnType: Int): ColumnDefinitionMessage {
        return ColumnDefinitionMessage(
            "root",
            "root",
            "users",
            "users",
            name,
            name,
            -1,
            0,
            columnType,
            0,
            0,
            registry.binaryDecoderFor(columnType, 3),
            registry.textDecoderFor(columnType, 3)
        )
    }

}
