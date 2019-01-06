package com.github.jasync.sql.db.mysql.message.client

import com.github.jasync.sql.db.mysql.message.server.ColumnDefinitionMessage

data class PreparedStatementExecuteMessage(
    @Suppress("ArrayInDataClass")
    val statementId: ByteArray,
    val values: List<Any?>,
    val valuesToInclude: Set<Int>,
    val parameters: List<ColumnDefinitionMessage>
) : ClientMessage(ClientMessage.PreparedStatementExecute)
