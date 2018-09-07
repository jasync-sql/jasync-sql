package com.github.jasync.sql.db.postgresql.messages.frontend

import com.github.jasync.sql.db.column.ColumnEncoderRegistry

open class PreparedStatementMessage(val statementId: Int,
                                    override val kind: Int,
                                    val query: String,
                                    val values: List<Any?>,
                                    val encoderRegistry: ColumnEncoderRegistry
) : ClientMessage(kind) {
  val valueTypes: List<Int> = values.map { value ->
    encoderRegistry.kindOf(value)
  }
}
