package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.Message

object MessageParser {

  private val parsers = Map(
    'C' -> CommandCompleteParser,
    'D' -> DataRowParser,
    'E' -> ErrorParser,
    'K' -> BackendKeyDataParser,
    'N' -> NoticeParser,
    'R' -> AuthenticationStartupParser,
    'S' -> ParameterStatusParser,
    'T' -> RowDescriptionParser,
    'Z' -> ReadyForQueryParser
  )

  def parserFor(t: Char): MessageParser = {
    val option = this.parsers.get(t)

    if ( option.isDefined ) {
      option.get
    } else {
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