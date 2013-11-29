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

package com.github.mauricio.async.db.mysql.decoder

import com.github.mauricio.async.db.mysql.message.server.{PreparedStatementPrepareResponse, ServerMessage}
import com.github.mauricio.async.db.util.Log
import io.netty.buffer.ByteBuf
import com.github.mauricio.async.db.mysql.MySQLHelper

class PreparedStatementPrepareResponseDecoder extends MessageDecoder {

  final val log = Log.get[PreparedStatementPrepareResponseDecoder]

  def decode(buffer: ByteBuf): ServerMessage = {

    //val dump = MySQLHelper.dumpAsHex(buffer)
    //log.debug("prepared statement response dump is \n{}", dump)

    val statementId = Array[Byte]( buffer.readByte(), buffer.readByte(), buffer.readByte(), buffer.readByte() )
    val columnsCount = buffer.readUnsignedShort()
    val paramsCount = buffer.readUnsignedShort()

    // filler
    buffer.readByte()

    val warningCount = buffer.readShort()

    new PreparedStatementPrepareResponse(
      statementId = statementId,
      warningCount = warningCount,
      columnsCount = columnsCount,
      paramsCount = paramsCount
    )
  }

}
