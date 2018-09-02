
package com.github.mauricio.async.db.postgresql.messages.frontend

import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage

class PreparedStatementExecuteMessage(statementId: Int, query: String, values: List<Any>, encoderRegistry: ColumnEncoderRegistry)
  : PreparedStatementMessage(statementId, ServerMessage.Execute, query, values, encoderRegistry)