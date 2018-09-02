package com.github.mauricio.async.db.postgresql.util

import com.github.mauricio.async.db.postgresql.exceptions.InvalidArrayException
import com.github.mauricio.async.db.util.Log
import scala.collection.mutable
import scala.collection.mutable.StringBuilder

object ArrayStreamingParser {

  val log = Log.getByName(ArrayStreamingParser.getClass.getName)

  fun parse(content: String, delegate: ArrayStreamingParserDelegate) {

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
        escaping = false
      } else {
        char when {
          '{' if !quoted -> {
            delegate.arrayStarted
            opens += 1
          }
          '}' if !quoted -> {
            if (currentElement != null) {
              sendElementEvent(currentElement, quoted, delegate)
              currentElement = null
            }
            delegate.arrayEnded
            closes += 1
          }
          '"' -> {
            if (quoted) {
              sendElementEvent(currentElement, quoted, delegate)
              currentElement = null
              quoted = false
            } else {
              quoted = true
              currentElement = mutable.StringBuilder()
            }
          }
          ',' if !quoted -> {
            if (currentElement != null) {
              sendElementEvent(currentElement, quoted, delegate)
            }
            currentElement = null
          }
          '\\' -> {
            escaping = true
          }
          else -> {
            if (currentElement == null) {
              currentElement = mutable.StringBuilder()
            }
            currentElement.append(char)
          }
        }
      }

      index += 1
    }

    if (opens != closes) {
      throw InvalidArrayException("This array is unbalanced %s".format(content))
    }

  }

  fun sendElementEvent(builder: mutable.StringBuilder, quoted: Boolean, delegate: ArrayStreamingParserDelegate) {

    val value = builder.toString()

    if (!quoted && "NULL".equalsIgnoreCase(value)) {
      delegate.nullElementFound
    } else {
      delegate.elementFound(value)
    }

  }


}