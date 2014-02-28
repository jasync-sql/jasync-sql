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

package com.github.mauricio.async.db.mysql.message.client

import com.github.mauricio.async.db.KindedMessage

object ClientMessage {

  final val ClientProtocolVersion = 0x09
  final val Quit = 0x01
  final val Query = 0x03
  final val PreparedStatementPrepare = 0x16
  final val PreparedStatementExecute = 0x17
  final val PreparedStatement = 0x18
  final val AuthSwitchResponse = 0xfe

}

class ClientMessage ( val kind : Int ) extends KindedMessage