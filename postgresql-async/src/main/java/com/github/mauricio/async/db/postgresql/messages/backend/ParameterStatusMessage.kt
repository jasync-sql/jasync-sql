package com.github.mauricio.async.db.postgresql.messages.backend

data class ParameterStatusMessage(val key: String, val value: String) : ServerMessage(ServerMessage.ParameterStatus)