package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import collection.mutable.ListBuffer
import com.github.mauricio.postgresql.ChannelUtils
import com.github.mauricio.postgresql.messages.backend.{InformationMessage, NoticeMessage, Message}

/**
 * User: Maurício Linhares
 * Date: 3/1/12
 * Time: 10:06 PM
 */

object NoticeParser extends InformationParser {

  def createMessage(fields: Map[String, String]): Message = new NoticeMessage(fields)

}
