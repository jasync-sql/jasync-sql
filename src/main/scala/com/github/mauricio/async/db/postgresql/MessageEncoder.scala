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

package com.github.mauricio.postgresql

import com.github.mauricio.async.db.postgresql.encoders._
import com.github.mauricio.async.db.postgresql.exceptions.EncoderNotAvailableException
import com.github.mauricio.async.db.postgresql.messages.frontend._
import com.github.mauricio.async.db.util.Log
import java.nio.charset.Charset
import org.jboss.netty.buffer.ChannelBuffer
import org.jboss.netty.channel.{Channel, ChannelHandlerContext}
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder
import com.github.mauricio.async.db.postgresql.column.ColumnEncoderRegistry

object MessageEncoder {
  val log = Log.get[MessageEncoder]
}

class MessageEncoder(charset: Charset, encoderRegistry : ColumnEncoderRegistry) extends OneToOneEncoder {

  import MessageEncoder.log

  val encoders: Map[Class[_], Encoder] = Map(
    classOf[CloseMessage] -> CloseMessageEncoder,
    classOf[PreparedStatementExecuteMessage] -> new ExecutePreparedStatementEncoder(charset, encoderRegistry),
    classOf[PreparedStatementOpeningMessage] -> new PreparedStatementOpeningEncoder(charset, encoderRegistry),
    classOf[StartupMessage] -> new StartupMessageEncoder(charset),
    classOf[QueryMessage] -> new QueryMessageEncoder(charset),
    classOf[CredentialMessage] -> new CredentialEncoder(charset)
  )

  override def encode(ctx: ChannelHandlerContext, channel: Channel, msg: AnyRef): ChannelBuffer = {

    val buffer = msg match {
      case message: FrontendMessage => {
        val option = this.encoders.get(message.getClass)
        if (option.isDefined) {
          option.get.encode(message)
        } else {
          throw new EncoderNotAvailableException(message)
        }
      }
      case _ => {
        throw new IllegalArgumentException("Can not encode message %s".format(msg))
      }
    }

    buffer
  }

}
