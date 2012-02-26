package com.github.mauricio.postgresql

import org.jboss.netty.buffer.ChannelBuffer
import parsers.{ParserS, ParserE, ParserR}

object MessageParser {
  
  private val parsers = Map( 
      'E' -> new ParserE(),
      'R' -> ParserR.Instance,
      'S' -> new ParserS()
      )
  
  def parserFor( t : Char ) : MessageParser = {
    this.parsers.get(t).getOrElse { 
      throw new ParserNotAvailableException(t)
    }
  }
  
  def parse( t : Char, b : ChannelBuffer ) : Message = {
    this.parserFor( t ).parseMessage( b )
  }
  
}

trait MessageParser {

  def parseMessage( buffer : ChannelBuffer ) : Message
  
}

class ParserNotAvailableException ( t : Char )
  extends RuntimeException( "There is no parser available for message type '%s'".format(t) )