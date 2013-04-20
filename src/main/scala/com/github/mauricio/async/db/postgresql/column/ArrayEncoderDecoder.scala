package com.github.mauricio.async.db.postgresql.column

import com.github.mauricio.async.db.util.{ArrayStreamingParser, ArrayStreamingParserDelegate}
import com.github.mauricio.postgresql.column.ColumnEncoderDecoder
import scala.collection.IndexedSeq
import scala.collection.mutable.{ArrayBuffer,Stack}

/**
 * User: mauricio
 * Date: 4/19/13
 * Time: 12:36 PM
 */
class ArrayEncoderDecoder( private val encoder : ColumnEncoderDecoder ) extends ColumnEncoderDecoder {

  override def decode(value: String): IndexedSeq[Any] = {

    val stack = new Stack[ArrayBuffer[Any]]()
    var current : ArrayBuffer[Any] = null
    var result : IndexedSeq[Any] = null
    val delegate = new ArrayStreamingParserDelegate {
      override def arrayEnded {
        result = stack.pop()
      }

      override def elementFound(element: String) {
        current += encoder.decode(element)
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

        stack.push( current )
      }
    }

    ArrayStreamingParser.parse(value, delegate)

    result
  }

  override def encode(value: Any): String = ???

}
