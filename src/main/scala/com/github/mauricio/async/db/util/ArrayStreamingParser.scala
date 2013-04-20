package com.github.mauricio.async.db.util

import scala.collection.mutable.StringBuilder
import scala.collection.mutable
import com.github.mauricio.async.db.postgresql.exceptions.InvalidArrayException

/**
 * User: mauricio
 * Date: 4/19/13
 * Time: 1:31 PM
 */
object ArrayStreamingParser {

  def parse(content: String, delegate: ArrayStreamingParserDelegate) {

    var index = 0
    var escaping = false
    var quoted = false
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
              sendElementEvent(currentElement, quoted, delegate)
              currentElement = null
            }
            delegate.arrayEnded
            closes += 1
          }
          case '"' => {
            if (quoted) {
              sendElementEvent(currentElement, quoted, delegate)
              currentElement = null
              quoted = false
            } else {
              quoted = true
              currentElement = new mutable.StringBuilder()
            }
          }
          case ',' => {
            if ( currentElement != null ) {
              sendElementEvent(currentElement, quoted, delegate)
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

  def sendElementEvent( builder : mutable.StringBuilder, quoted : Boolean, delegate: ArrayStreamingParserDelegate  ) {

    val value = builder.toString()

    if ( !quoted && "NULL".equalsIgnoreCase(value) ) {
      delegate.nullElementFound
    } else {
      delegate.elementFound(value)
    }

  }


}
