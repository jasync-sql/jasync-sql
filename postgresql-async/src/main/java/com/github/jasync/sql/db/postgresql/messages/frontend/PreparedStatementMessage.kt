package com.github.jasync.sql.db.postgresql.messages.frontend

import com.github.jasync.sql.db.column.ColumnEncoderRegistry
import java.util.UUID

open class PreparedStatementMessage(
    val statementId: UUID,
    override val kind: Int,
    val query: String,
    val values: List<Any?>,
    val encoderRegistry: ColumnEncoderRegistry
) : ClientMessage(kind) {
    val valueTypes: List<Int> = values.map { value ->
        encoderRegistry.kindOf(value)
    }
}
