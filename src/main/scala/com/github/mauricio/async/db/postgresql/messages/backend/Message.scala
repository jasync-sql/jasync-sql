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

package com.github.mauricio.async.db.postgresql.messages.backend

object Message {
  val Authentication: Byte = 'R'
  val BackendKeyData: Byte = 'K'
  val Bind: Byte = 'B'
  val BindComplete: Byte = '2'
  val CommandComplete: Byte = 'C'
  val Close: Byte = 'X'
  val CloseStatementOrPortal: Byte = 'C'
  val CloseComplete: Byte = '3'
  val DataRow: Byte = 'D'
  val Describe: Byte = 'D'
  val Error: Byte = 'E'
  val Execute: Byte = 'E'
  val EmptyQueryString: Byte = 'I'
  val NoData: Byte = 'n'
  val Notice: Byte = 'N'
  val Notification: Byte = 'A'
  val ParameterStatus: Byte = 'S'
  val Parse: Byte = 'P'
  val ParseComplete: Byte = '1'
  val PasswordMessage: Byte = 'p'
  val PortalSuspended: Byte = 's'
  val Query: Byte = 'Q'
  val RowDescription: Byte = 'T'
  val ReadyForQuery: Byte = 'Z'
  val Startup: Byte = 0
  val Sync: Byte = 'S'
}

class Message(val name: Byte)