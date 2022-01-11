package com.github.jasync.sql.db.postgresql.messages.frontend

import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage

object CloseMessage : ClientMessage(ServerMessage.Close)
