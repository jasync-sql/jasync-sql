package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.Message

object MessageParser {

  private val parsers = Map(
    'C' -> ParserC,
    'E' -> ParserE,
    'K' -> ParserK,
    'N' -> ParserN,
    'R' -> ParserR,
    'S' -> ParserS,
    'T' -> ParserT,
    'Z' -> ParserZ
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