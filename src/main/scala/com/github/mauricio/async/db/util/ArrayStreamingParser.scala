package com.github.mauricio.async.db.util

import scala.collection.mutable.StringBuilder
import scala.collection.mutable
import com.github.mauricio.async.db.postgresql.exceptions.InvalidArrayException

/**
 * User: mauricio
 * Date: 4/19/13
 * Time: 1:31 PM
 */
class ArrayStreamingParser {

  def parse(content: String, delegate: ArrayStreamingParserDelegate) {

    var index = 0
    var escaping = false
    var elementOpened = false
    var currentElement: StringBuilder = null
    var opens = 0
    var closes = 0

    while (index < content.size) {
      val char = content.charAt(index)
      if (escaping) {
        currentElement.append(char)
        escaping  = false
      } else {
        char match {
          case '{' => {
            delegate.arrayStarted
            opens += 1
          }
          case '}' => {
            if (currentElement != null) {
              delegate.elementFound(currentElement.toString())
              currentElement = null
            }
            delegate.arrayEnded
            closes += 1
          }
          case '"' => {
            if (elementOpened) {
              delegate.elementFound(currentElement.toString())
              currentElement = null
              elementOpened = false
            } else {
              elementOpened = true
              currentElement = new mutable.StringBuilder()
            }
          }
          case ',' => {
            if ( currentElement != null ) {
              delegate.elementFound(currentElement.toString())
            }
            currentElement = null
          }
          case '\\' => {
            escaping = true
          }
          case _ => {
            if ( currentElement == null ) {
              currentElement = new mutable.StringBuilder()
            }
            currentElement.append(char)
          }
        }
      }

      index += 1
    }

    if ( opens != closes ) {
      throw new InvalidArrayException("This array is unbalanced %s".format(content))
    }

  }


}
