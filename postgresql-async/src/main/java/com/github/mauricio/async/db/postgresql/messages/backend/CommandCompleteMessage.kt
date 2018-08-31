package com.github.mauricio.async.db.postgresql.messages.backend

data class CommandCompleteMessage(val rowsAffected: Int, val statusMessage: String) : ServerMessage(ServerMessage.CommandComplete)