package com.github.jasync.sql.db.postgresql.messages.frontend

import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import java.util.UUID

class PreparedStatementOpeningMessage(
    statementId: UUID,
    query: String,
    values: List<Any?>,
    encoderRegistry: ColumnEncoderRegistry
) : PreparedStatementMessage(statementId, ServerMessage.Parse, query, values, encoderRegistry) {

    override fun toString(): String = "${this.javaClass.simpleName}(id=$statementId,query=$query,values=$values})"
}
