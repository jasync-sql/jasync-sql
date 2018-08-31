package com.github.mauricio.async.db.postgresql.column

import com.github.mauricio.async.db.column.ColumnDecoder
import com.github.mauricio.async.db.postgresql.util.{ArrayStreamingParserDelegate, ArrayStreamingParser}
import scala.collection.IndexedSeq
import scala.collection.mutable.ArrayBuffer
import com.github.mauricio.async.db.general.ColumnData
import io.netty.buffer.{Unpooled, ByteBuf}
import java.nio.charset.Charset

class ArrayDecoder(private val decoder: ColumnDecoder) extends ColumnDecoder {

  override def decode( kind : ColumnData, buffer : ByteBuf, charset : Charset ): IndexedSeq[Any] = {

    val bytes = new Array[Byte](buffer.readableBytes())
    buffer.readBytes(bytes)
    val value = new String(bytes, charset)

    var stack = List.empty[ArrayBuffer[Any]]
    var current: ArrayBuffer[Any] = null
    var result: IndexedSeq[Any] = null
    val delegate = new ArrayStreamingParserDelegate {
      override def arrayEnded {
        result = stack.head
        stack = stack.tail
      }

      override def elementFound(element: String) {
        val result = if ( decoder.supportsStringDecoding ) {
          decoder.decode(element)
        } else {
          decoder.decode(kind, Unpooled.wrappedBuffer( element.getBytes(charset) ), charset)
        }
        current += result
      }

      override def nullElementFound {
        current += null
      }

      override def arrayStarted {
        current = new ArrayBuffer[Any]()

        stack.headOption match {
          case Some(item) => {
            item += current
          }
          case None => {}
        }

        stack ::= current
      }
    }

    ArrayStreamingParser.parse(value, delegate)

    result
  }

  def decode( value : String ) : Any = throw new UnsupportedOperationException("Should not be called")

}
