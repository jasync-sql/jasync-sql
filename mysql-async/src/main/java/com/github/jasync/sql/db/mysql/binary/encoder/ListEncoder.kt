package com.github.jasync.sql.db.mysql.binary.encoder

import com.github.jasync.sql.db.mysql.column.ColumnTypes
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

/**
 * Encoder for List types (including ArrayList) in MySQL prepared statements.
 * This encoder converts the list to a comma-separated string for use in SQL IN clauses.
 */
class ListEncoder(private val charset: Charset) : BinaryEncoder {

    override fun encodesTo(): Int = ColumnTypes.FIELD_TYPE_VAR_STRING

    override fun encode(value: Any, buffer: ByteBuf) {
        if (value !is List<*>) {
            throw IllegalArgumentException("Cannot encode non-List value with ListEncoder")
        }

        // Convert list to comma-separated string
        val stringValue = value.filterNotNull().joinToString(",")
        
        // Write the string as length-encoded string
        val bytes = stringValue.toByteArray(charset)
        
        // MySQL uses length coded binary for strings
        // https://dev.mysql.com/doc/internals/en/string.html
        if (bytes.size < 251) {
            buffer.writeByte(bytes.size)
        } else if (bytes.size < 65536) {
            buffer.writeByte(252)
            buffer.writeShortLE(bytes.size)
        } else if (bytes.size < 16777216) {
            buffer.writeByte(253)
            buffer.writeMediumLE(bytes.size)
        } else {
            buffer.writeByte(254)
            buffer.writeLongLE(bytes.size.toLong())
        }
        
        buffer.writeBytes(bytes)
    }
}

/**
 * Helper method to write a 3-byte integer in little-endian format
 */
private fun ByteBuf.writeMediumLE(value: Int) {
    this.writeByte(value and 0xFF)
    this.writeByte(value shr 8 and 0xFF)
    this.writeByte(value shr 16 and 0xFF)
}
