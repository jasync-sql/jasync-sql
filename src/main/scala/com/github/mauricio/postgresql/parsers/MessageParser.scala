package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.messages.backend.{CloseComplete, BindComplete, ParseComplete, Message}
import com.github.mauricio.postgresql.exceptions.ParserNotAvailableException

object MessageParser {

  private val parsers = Map(
    Message.Authentication -> AuthenticationStartupParser,
    Message.BackendKeyData -> BackendKeyDataParser,
    Message.BindComplete -> new ReturningMessageParser(BindComplete.Instance),
    Message.CloseComplete -> new ReturningMessageParser(CloseComplete.Instance),
    Message.CommandComplete -> CommandCompleteParser,
    Message.DataRow -> DataRowParser,
    Message.Error -> ErrorParser,
    Message.Notice -> NoticeParser,
    Message.ParameterStatus -> ParameterStatusParser,
    Message.ParseComplete -> new ReturningMessageParser(ParseComplete.Instance),
    Message.RowDescription -> RowDescriptionParser,
    Message.ReadyForQuery -> ReadyForQueryParser
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