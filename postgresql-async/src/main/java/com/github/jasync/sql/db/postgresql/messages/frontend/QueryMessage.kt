package com.github.jasync.sql.db.postgresql.messages.frontend

import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage

class QueryMessage(val query: String) : ClientMessage(ServerMessage.Query)
