package com.github.mauricio.postgresql.parsers

import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.postgresql.messages.backend.{CloseComplete, BindComplete, ParseComplete, Message}
import com.github.mauricio.postgresql.exceptions.ParserNotAvailableException
import java.nio.charset.Charset

class MessageParser(charset : Charset) {

  private val parsers = Map(
    Message.Authentication -> AuthenticationStartupParser,
    Message.BackendKeyData -> BackendKeyDataParser,
    Message.BindComplete -> new ReturningMessageParser(BindComplete.Instance),
    Message.CloseComplete -> new ReturningMessageParser(CloseComplete.Instance),
    Message.CommandComplete -> new CommandCompleteParser(charset),
    Message.DataRow -> DataRowParser,
    Message.Error -> new ErrorParser(charset),
    Message.Notice -> new NoticeParser(charset),
    Message.ParameterStatus -> new ParameterStatusParser(charset),
    Message.ParseComplete -> new ReturningMessageParser(ParseComplete.Instance),
    Message.RowDescription -> new RowDescriptionParser(charset),
    Message.ReadyForQuery -> ReadyForQueryParser
  )

  def parserFor(t: Char): Decoder = {
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