package com.github.jasync.sql.db.postgresql.util

import com.github.jasync.sql.db.postgresql.exceptions.InvalidArrayException
import com.github.jasync.sql.db.util.size
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

object ArrayStreamingParser {


    fun parse(content: String, delegate: ArrayStreamingParserDelegate) {

        var index = 0
        var escaping = false
        var quoted = false
        var currentElement: StringBuilder? = null
        var opens = 0
        var closes = 0

        while (index < content.size) {
            val char = content[index]

            if (escaping) {
                currentElement!!.append(char)
                escaping = false
            } else {
                when {
                    char == '{' && !quoted -> {
                        delegate.arrayStarted()
                        opens += 1
                    }
                    char == '}' && !quoted -> {
                        if (currentElement != null) {
                            sendElementEvent(currentElement, quoted, delegate)
                            currentElement = null
                        }
                        delegate.arrayEnded()
                        closes += 1
                    }
                    char == '"' -> {
                        if (quoted) {
                            sendElementEvent(currentElement, quoted, delegate)
                            currentElement = null
                            quoted = false
                        } else {
                            quoted = true
                            currentElement = StringBuilder()
                        }
                    }
                    char == ',' && !quoted -> {
                        if (currentElement != null) {
                            sendElementEvent(currentElement, quoted, delegate)
                        }
                        currentElement = null
                    }
                    char == '\\' -> {
                        escaping = true
                    }
                    else -> {
                        if (currentElement == null) {
                            currentElement = StringBuilder()
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

    fun sendElementEvent(builder: StringBuilder?, quoted: Boolean, delegate: ArrayStreamingParserDelegate) {

        val value = builder.toString()

        if (!quoted && "NULL".equals(value, ignoreCase = true)) {
            delegate.nullElementFound()
        } else {
            delegate.elementFound(value)
        }

    }


}
