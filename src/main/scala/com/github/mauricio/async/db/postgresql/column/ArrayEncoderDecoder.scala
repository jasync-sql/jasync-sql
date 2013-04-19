package com.github.mauricio.async.db.postgresql.column

import com.github.mauricio.postgresql.column.ColumnEncoderDecoder
import com.github.mauricio.async.db.postgresql.exceptions.InvalidArrayException
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer

/**
 * User: mauricio
 * Date: 4/19/13
 * Time: 12:36 PM
 */
class ArrayEncoderDecoder( private val encoder : ColumnEncoderDecoder ) extends ColumnEncoderDecoder {

  override def decode(value: String): Any = {

    if ( value.charAt(0) != '{' || value.charAt(value.size - 1) != '}' ) {
      throw new InvalidArrayException("The array %s is not valid".format(value))
    }

    var index = 0
    var escaping = false
    val stack = new mutable.Stack[ArrayBuffer[Any]]()
    var currentValue : mutable.StringBuilder = null

    while ( index < value.size ) {

      val item = value.charAt(index)

      if ( escaping ) {
        currentValue.append(item)
        escaping = false
      } else {
        item match {
          case '{' => {
            val currentArray = new ArrayBuffer[Any]()

            if ( !stack.isEmpty ) {
              stack.top += currentArray
            }

            stack.push( currentArray )
          }
          case '}' => stack.pop()
          case '"' => currentValue = new mutable.StringBuilder()
        }
      }

      index += 1

    }

  }

  override def encode(value: Any): String = ???

}
