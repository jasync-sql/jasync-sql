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

import com.github.jasync.sql.db.KindedMessage

abstract class ServerMessage(override val kind: Int) : KindedMessage {
  companion object {
    const val Authentication = 'R'.toInt()
    const val BackendKeyData = 'K'.toInt()
    const val Bind = 'B'.toInt()
    const val BindComplete = '2'.toInt()
    const val CommandComplete = 'C'.toInt()
    const val Close = 'X'.toInt()
    const val CloseStatementOrPortal = 'C'.toInt()
    const val CloseComplete = '3'.toInt()
    const val DataRow = 'D'.toInt()
    const val Describe = 'D'.toInt()
    const val Error = 'E'.toInt()
    const val Execute = 'E'.toInt()
    const val EmptyQueryString = 'I'.toInt()
    const val NoData = 'n'.toInt()
    const val Notice = 'N'.toInt()
    const val NotificationResponse = 'A'.toInt()
    const val ParameterStatus = 'S'.toInt()
    const val Parse = 'P'.toInt()
    const val ParseComplete = '1'.toInt()
    const val PasswordMessage = 'p'.toInt()
    const val PortalSuspended = 's'.toInt()
    const val Query = 'Q'.toInt()
    const val RowDescription = 'T'.toInt()
    const val ReadyForQuery = 'Z'.toInt()
    const val Sync = 'S'.toInt()
  }
}