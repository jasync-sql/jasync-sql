package com.github.jasync.sql.db.mysql.message.server

data class PreparedStatementPrepareResponse(
    @Suppress("ArrayInDataClass")
    val statementId: ByteArray,
    val warningCount: Short,
    val paramsCount: Int,
    val columnsCount: Int
) : ServerMessage(ServerMessage.PreparedStatementPrepareResponse)
