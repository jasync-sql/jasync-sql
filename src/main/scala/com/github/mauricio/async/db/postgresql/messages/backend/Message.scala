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
  val Authentication = 'R'
  val BackendKeyData = 'K'
  val Bind = 'B'
  val BindComplete = '2'
  val CommandComplete = 'C'
  val Close = 'X'
  val CloseStatementOrPortal = 'C'
  val CloseComplete = '3'
  val DataRow = 'D'
  val Describe = 'D'
  val Error = 'E'
  val Execute = 'E'
  val EmptyQuery = 'I'
  val NoData = 'n'
  val Notice = 'N'
  val Notification = 'A'
  val ParameterStatus = 'S'
  val Parse = 'P'
  val ParseComplete = '1'
  val PasswordMessage = 'p'
  val PortalSuspended = 's'
  val Query = 'Q'
  val RowDescription = 'T'
  val ReadyForQuery = 'Z'
  val Startup: Char = 0
  val Sync = 'S'
}

class Message(val name: Char)