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

import com.github.mauricio.async.db.postgresql.messages.backend._
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer
import com.github.mauricio.async.db.exceptions.ParserNotAvailableException

class MessageParsersRegistry(charset: Charset) {

  private val commandCompleteParser = new CommandCompleteParser(charset)
  private val errorParser = new ErrorParser(charset)
  private val noticeParser = new NoticeParser(charset)
  private val parameterStatusParser = new ParameterStatusParser(charset)
  private val rowDescriptionParser = new RowDescriptionParser(charset)

  private def parserFor(t: Byte): MessageParser = {
    t match {
      case ServerMessage.Authentication => AuthenticationStartupParser
      case ServerMessage.BackendKeyData => BackendKeyDataParser
      case ServerMessage.BindComplete => ReturningMessageParser.BindCompleteMessageParser
      case ServerMessage.CloseComplete => ReturningMessageParser.CloseCompleteMessageParser
      case ServerMessage.CommandComplete => this.commandCompleteParser
      case ServerMessage.DataRow => DataRowParser
      case ServerMessage.Error => this.errorParser
      case ServerMessage.EmptyQueryString => ReturningMessageParser.EmptyQueryStringMessageParser
      case ServerMessage.NoData => ReturningMessageParser.NoDataMessageParser
      case ServerMessage.Notice => this.noticeParser
      case ServerMessage.ParameterStatus => this.parameterStatusParser
      case ServerMessage.ParseComplete => ReturningMessageParser.ParseCompleteMessageParser
      case ServerMessage.RowDescription => this.rowDescriptionParser
      case ServerMessage.ReadyForQuery => ReadyForQueryParser
      case _ => throw new ParserNotAvailableException(t)
    }
  }

  def parse(t: Byte, b: ChannelBuffer): ServerMessage = {
    this.parserFor(t).parseMessage(b)
  }

}