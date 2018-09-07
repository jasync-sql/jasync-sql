package com.github.jasync.sql.db.postgresql.messages.backend

data class CommandCompleteMessage(val rowsAffected: Int, val statusMessage: String) : ServerMessage(ServerMessage.CommandComplete)
