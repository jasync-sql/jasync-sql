package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.{QueryResult, ChannelUtils, Message}

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 10:33 PM
 */

object ParserC extends MessageParser {

  override def parseMessage(b: ChannelBuffer): Message = {

    val result = ChannelUtils.readCString(b)

    val possibleRowCount = result.splitAt( result.lastIndexOf(" ") )

    val rowCount : Int = try {
      possibleRowCount._2.toInt
    } catch {
      case e : Exception => {
        0
      }
    }

    new Message( Message.CommandComplete, new QueryResult( rowCount ) )
  }

}
