package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.Message

object MessageParser {

  private val parsers = Map(
    'E' -> new ParserE(),
    'R' -> ParserR.Instance,
    'S' -> new ParserS(),
    'K' -> new ParserK(),
    'Z' -> new ParserZ()
  )

  def parserFor(t: Char): MessageParser = {
    this.parsers.get(t).getOrElse {
      throw new ParserNotAvailableException(t)
    }
  }

  def parse(t: Char, b: ChannelBuffer): Message = {
    this.parserFor(t).parseMessage(b)
  }

}

trait MessageParser {

  def parseMessage(buffer: ChannelBuffer): Message

}

class ParserNotAvailableException(t: Char)
  extends RuntimeException("There is no parser available for message type '%s'".format(t))