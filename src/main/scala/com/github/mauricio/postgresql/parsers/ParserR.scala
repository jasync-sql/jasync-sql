package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.Message

object ParserR {
  private[parsers] val AUTH_REQ_OK = 0
  private[parsers] val AUTH_REQ_PASSWORD = 3

  val Instance = new ParserR();
}

class ParserR extends MessageParser {

  import ParserR._

  override def parseMessage(b: ChannelBuffer): Message = {

    val authenticationType = b.readInt()

    authenticationType match {
      case AUTH_REQ_OK => {
        new Message( Message.AuthenticationOk, authenticationType)
      }
      case AUTH_REQ_PASSWORD => {
        new Message( Message.AuthenticationOk , authenticationType)
      }
      case _ => {
        throw new IllegalArgumentException("Unknown authentication method -> '%s'".format(authenticationType))
      }

    }

  }

}