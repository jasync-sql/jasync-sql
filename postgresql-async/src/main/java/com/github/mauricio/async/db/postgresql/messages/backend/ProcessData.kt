package com.github.mauricio.async.db.postgresql.messages.backend

data class ProcessData(val processId: Int, val secretKey: Int) : ServerMessage(ServerMessage.BackendKeyData)