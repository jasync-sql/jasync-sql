package com.github.jasync.sql.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnDecoder
import com.github.jasync.sql.db.general.ColumnData
import com.github.jasync.sql.db.postgresql.util.ArrayStreamingParser
import com.github.jasync.sql.db.postgresql.util.ArrayStreamingParserDelegate
import com.github.jasync.sql.db.util.head
import com.github.jasync.sql.db.util.tail
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.charset.Charset

class ArrayDecoder(private val decoder: ColumnDecoder) : ColumnDecoder {

    override fun decode(kind: ColumnData, value: ByteBuf, charset: Charset): List<Any?>? {
        val bytes = ByteArray(value.readableBytes())
        value.readBytes(bytes)
        val valueString = String(bytes, charset)

        var stack = mutableListOf<MutableList<Any?>?>()
        var current: MutableList<Any?>? = null
        var result: List<Any?>? = null
        val delegate = object : ArrayStreamingParserDelegate {
            override fun arrayEnded() {
                result = stack.head
                stack = stack.tail.toMutableList()
            }

            override fun elementFound(element: String) {
                val foundResult = if (decoder.supportsStringDecoding()) {
                    decoder.decode(element)
                } else {
                    decoder.decode(kind, Unpooled.wrappedBuffer(element.toByteArray(charset)), charset)
                }
                current!!.add(foundResult)
            }

            override fun nullElementFound() {
                current!!.add(null)
            }

            override fun arrayStarted() {
                current = mutableListOf()

                if (stack.isNotEmpty()) {
                    stack.head!!.add(current)
                }

                stack.add(0, current)
            }
        }

        ArrayStreamingParser.parse(valueString, delegate)

        return result
    }

    override fun decode(value: String): Any = throw UnsupportedOperationException("Should not be called")
}
