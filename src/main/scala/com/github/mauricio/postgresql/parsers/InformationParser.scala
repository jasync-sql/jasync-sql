package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.messages.backend.{NoticeMessage, InformationMessage, Message}
import com.github.mauricio.postgresql.ChannelUtils

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 12:54 AM
 */
abstract class InformationParser extends MessageParser {

  override def parseMessage(b: ChannelBuffer): Message = {

    val fields = scala.collection.mutable.Map[String,String]()

    while ( b.readable() ) {
      val kind = b.readByte()

      if ( kind != 0 ) {
        fields.put(
          InformationMessage.fieldName(kind.asInstanceOf[Char]),
          ChannelUtils.readCString(b)
        )
      }

    }

    createMessage(fields.toMap)
  }

  def createMessage( fields : Map[String,String] ) : Message

}
