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

import com.github.mauricio.async.db.KindedMessage

object ServerMessage {
  final val Authentication = 'R'
  final val BackendKeyData = 'K'
  final val Bind = 'B'
  final val BindComplete = '2'
  final val CommandComplete = 'C'
  final val Close = 'X'
  final val CloseStatementOrPortal = 'C'
  final val CloseComplete = '3'
  final val DataRow = 'D'
  final val Describe = 'D'
  final val Error = 'E'
  final val Execute = 'E'
  final val EmptyQueryString = 'I'
  final val NoData = 'n'
  final val Notice = 'N'
  final val NotificationResponse = 'A'
  final val ParameterStatus = 'S'
  final val Parse = 'P'
  final val ParseComplete = '1'
  final val PasswordMessage = 'p'
  final val PortalSuspended = 's'
  final val Query = 'Q'
  final val RowDescription = 'T'
  final val ReadyForQuery = 'Z'
  final val Sync = 'S'
}

class ServerMessage(val kind: Int) extends KindedMessage