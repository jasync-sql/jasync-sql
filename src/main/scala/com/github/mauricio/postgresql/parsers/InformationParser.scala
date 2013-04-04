package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.messages.backend.Message
import com.github.mauricio.postgresql.ChannelUtils
import java.nio.charset.Charset

/**
 * User: mauricio
 * Date: 3/31/13
 * Time: 12:54 AM
 */
abstract class InformationParser( charset : Charset ) extends Decoder {

  override def parseMessage(b: ChannelBuffer): Message = {

    val fields = scala.collection.mutable.Map[Char,String]()

    while ( b.readable() ) {
      val kind = b.readByte()

      if ( kind != 0 ) {
        fields.put(
          kind.toChar,
          ChannelUtils.readCString(b, charset)
        )
      }

    }

    createMessage(fields.toMap)
  }

  def createMessage( fields : Map[Char,String] ) : Message

}
