package com.github.jasync.sql.db.postgresql.messages.backend

data class ProcessData(val processId: Int, val secretKey: Int) : ServerMessage(ServerMessage.BackendKeyData)
