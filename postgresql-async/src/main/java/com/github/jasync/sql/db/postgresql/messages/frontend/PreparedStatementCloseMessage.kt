package com.github.jasync.sql.db.postgresql.messages.frontend

import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage

data class PreparedStatementCloseMessage(val statementId: Int, val isStatement: Boolean = true) :
    ClientMessage(ServerMessage.CloseStatementOrPortal)