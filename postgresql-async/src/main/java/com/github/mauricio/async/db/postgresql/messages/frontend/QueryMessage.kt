package com.github.mauricio.async.db.postgresql.messages.frontend

import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage

class QueryMessage(val query: String) : ClientMessage(ServerMessage.Query)