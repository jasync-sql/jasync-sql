package com.github.mauricio.async.db.postgresql.messages.frontend

import com.github.mauricio.async.db.postgresql.messages.backend.ServerMessage

object CloseMessage : ClientMessage(ServerMessage.Close)