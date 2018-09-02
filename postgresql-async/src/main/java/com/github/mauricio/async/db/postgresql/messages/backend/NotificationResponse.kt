package com.github.mauricio.async.db.postgresql.messages.backend

class NotificationResponse(val backendPid: Int, val channel: String, val payload: String) : ServerMessage(ServerMessage.NotificationResponse)