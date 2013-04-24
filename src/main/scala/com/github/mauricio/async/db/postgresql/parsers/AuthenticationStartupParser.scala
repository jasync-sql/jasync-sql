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

import com.github.mauricio.async.db.postgresql.exceptions.UnsupportedAuthenticationMethodException
import com.github.mauricio.async.db.postgresql.messages.backend.{AuthenticationChallengeMD5, AuthenticationChallengeCleartextMessage, AuthenticationOkMessage, Message}
import org.jboss.netty.buffer.ChannelBuffer

object AuthenticationStartupParser extends MessageParser {

  val AuthenticationOk = 0
  val AuthenticationKerberosV5 = 2
  val AuthenticationCleartextPassword = 3
  val AuthenticationMD5Password = 5
  val AuthenticationSCMCredential = 6
  val AuthenticationGSS = 7
  val AuthenticationGSSContinue = 8
  val AuthenticationSSPI = 9

  override def parseMessage(b: ChannelBuffer): Message = {

    val authenticationType = b.readInt()

    authenticationType match {
      case AuthenticationOk => AuthenticationOkMessage.Instance
      case AuthenticationCleartextPassword => AuthenticationChallengeCleartextMessage.Instance
      case AuthenticationMD5Password => {
        val bytes = new Array[Byte](b.readableBytes())
        b.readBytes(bytes)
        new AuthenticationChallengeMD5(bytes)
      }
      case _ => {
        throw new UnsupportedAuthenticationMethodException(authenticationType)
      }

    }

  }

}