package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.ChannelUtils
import com.github.mauricio.postgresql.messages.backend.{CommandCompleteMessage, Message}
import java.nio.charset.Charset

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 10:33 PM
 */

class CommandCompleteParser (charset : Charset) extends Decoder {

  override def parseMessage(b: ChannelBuffer): Message = {

    val result = ChannelUtils.readCString(b, charset)

    val possibleRowCount = result.substring(result.lastIndexOf(" "))

    val rowCount : Int = try {
      possibleRowCount.toInt
    } catch {
      case e : NumberFormatException => {
        0
      }
    }

    new CommandCompleteMessage(rowCount, result)
  }

}
