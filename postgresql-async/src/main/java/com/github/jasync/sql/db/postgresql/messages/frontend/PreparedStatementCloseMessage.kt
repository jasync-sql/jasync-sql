package com.github.jasync.sql.db.postgresql.messages.frontend

import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import java.util.*

data class PreparedStatementCloseMessage(val statementId: UUID, val isStatement: Boolean = true) :
    ClientMessage(ServerMessage.CloseStatementOrPortal)