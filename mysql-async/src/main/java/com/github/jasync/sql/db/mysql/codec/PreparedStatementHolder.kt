package com.github.jasync.sql.db.mysql.codec

import com.github.jasync.sql.db.mysql.message.server.ColumnDefinitionMessage
import com.github.jasync.sql.db.mysql.message.server.PreparedStatementPrepareResponse
import com.github.jasync.sql.db.util.length

class PreparedStatementHolder(val statement: String, val message: PreparedStatementPrepareResponse) {

    val columns = mutableListOf<ColumnDefinitionMessage>()
    val parameters = mutableListOf<ColumnDefinitionMessage>()

    fun statementId(): ByteArray = message.statementId

    private fun needsParameters(): Boolean = message.paramsCount != this.parameters.length

    private fun needsColumns(): Boolean = message.columnsCount != this.columns.length

    fun needsAny(): Boolean = this.needsParameters() || this.needsColumns()

    fun add(column: ColumnDefinitionMessage) {
        if (this.needsParameters()) {
            this.parameters += column
        } else {
            if (this.needsColumns()) {
                this.columns += column
            }
        }
    }

    override fun toString(): String = "PreparedStatementHolder($statement)"

}
