package com.github.mauricio.async.db.postgresql.column

import com.github.jasync.sql.db.column.ColumnDecoder
import com.github.jasync.sql.db.general.ColumnData
import com.github.jasync.sql.db.util.head
import com.github.jasync.sql.db.util.tail
import com.github.mauricio.async.db.postgresql.util.ArrayStreamingParser
import com.github.mauricio.async.db.postgresql.util.ArrayStreamingParserDelegate
import io.netty.buffer.ByteBuf
import io.netty.buffer.Unpooled
import java.nio.charset.Charset

class ArrayDecoder(private val decoder: ColumnDecoder) : ColumnDecoder {

  override fun decode(kind : ColumnData, buffer : ByteBuf, charset : Charset ): List<Any> {

    val bytes = ByteArray(buffer.readableBytes())
    buffer.readBytes(bytes)
    val value = String(bytes, charset)

    var stack = emptyList<Array<Any>>
    var current: ArrayBuffer<Any> = null
    var result: List<Any> = null
    val delegate = ArrayStreamingParserDelegate {
      override fun arrayEnded() {
        result = stack.head
        stack = stack.tail
      }

      override fun elementFound(element: String) {
        val result = if ( decoder.supportsStringDecoding ) {
          decoder.decode(element)
        } else {
          decoder.decode(kind, Unpooled.wrappedBuffer( element.getBytes(charset) ), charset)
        }
        current += result
      }

      override fun nullElementFound {
        current += null
      }

      override fun arrayStarted {
        current = ArrayBuffer<Any>()

        stack.headOption when {
          Some(item) -> {
            item += current
          }
          None -> {}
        }

        stack ::= current
      }
    }

    ArrayStreamingParser.parse(value, delegate)

    result
  }

  fun decode( value : String ) : Any = throw UnsupportedOperationException("Should not be called")

}
