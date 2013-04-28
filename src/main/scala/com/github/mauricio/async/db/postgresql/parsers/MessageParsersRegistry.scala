/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.postgresql.parsers

import com.github.mauricio.async.db.postgresql.exceptions.ParserNotAvailableException
import com.github.mauricio.async.db.postgresql.messages.backend._
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer

class MessageParsersRegistry(charset: Charset) {

  private val parsers = Map(
    Message.Authentication -> AuthenticationStartupParser,
    Message.BackendKeyData -> BackendKeyDataParser,
    Message.BindComplete -> new ReturningMessageParser(BindComplete.Instance),
    Message.CloseComplete -> new ReturningMessageParser(CloseComplete.Instance),
    Message.CommandComplete -> new CommandCompleteParser(charset),
    Message.DataRow -> DataRowParser,
    Message.Error -> new ErrorParser(charset),
    Message.EmptyQueryString -> new ReturningMessageParser(EmptyQueryString),
    Message.NoData -> new ReturningMessageParser(NoData),
    Message.Notice -> new NoticeParser(charset),
    Message.ParameterStatus -> new ParameterStatusParser(charset),
    Message.ParseComplete -> new ReturningMessageParser(ParseComplete.Instance),
    Message.RowDescription -> new RowDescriptionParser(charset),
    Message.ReadyForQuery -> ReadyForQueryParser
  )

  private def parserFor(t: Char): MessageParser = {
    val option = this.parsers.get(t)

    if (option.isDefined) {
      option.get
    } else {
      throw new ParserNotAvailableException(t)
    }

  }

  def parse(t: Char, b: ChannelBuffer): Message = {
    this.parserFor(t).parseMessage(b)
  }

}