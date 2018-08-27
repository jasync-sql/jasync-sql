
package com.github.mauricio.async.db.mysql.message.client

import com.github.mauricio.async.db.mysql.message.server.ColumnDefinitionMessage

data class PreparedStatementExecuteMessage (
                                            val  statementId : ByteArray,
                                            val values : List<Any?>,
                                            val valuesToInclude : Set<Int>,
                                            val parameters : List<ColumnDefinitionMessage> )
  : ClientMessage( ClientMessage.PreparedStatementExecute )
