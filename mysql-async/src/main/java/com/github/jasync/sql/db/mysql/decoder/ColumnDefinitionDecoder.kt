package com.github.jasync.sql.db.mysql.decoder

import com.github.jasync.sql.db.mysql.codec.DecoderRegistry
import com.github.jasync.sql.db.mysql.message.server.ColumnDefinitionMessage
import com.github.jasync.sql.db.util.readBinaryLength
import com.github.jasync.sql.db.util.readLengthEncodedString
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset

class ColumnDefinitionDecoder(val charset: Charset, val registry: DecoderRegistry) : MessageDecoder {

    override fun decode(buffer: ByteBuf): ColumnDefinitionMessage {
        val catalog = buffer.readLengthEncodedString(charset)
        val schema = buffer.readLengthEncodedString(charset)
        val table = buffer.readLengthEncodedString(charset)
        val originalTable = buffer.readLengthEncodedString(charset)
        val name = buffer.readLengthEncodedString(charset)
        val originalName = buffer.readLengthEncodedString(charset)

        buffer.readBinaryLength()

        val characterSet = buffer.readUnsignedShort()
        val columnLength = buffer.readUnsignedInt()
        val columnType = buffer.readUnsignedByte()
        val flags = buffer.readShort()
        val decimals = buffer.readByte()

        buffer.readShort()

        return ColumnDefinitionMessage(
            catalog,
            schema,
            table,
            originalTable,
            name,
            originalName,
            characterSet,
            columnLength,
            columnType.toInt(),
            flags,
            decimals,
            registry.binaryDecoderFor(columnType.toInt(), characterSet),
            registry.textDecoderFor(columnType.toInt(), characterSet)
        )
    }
}
