package com.github.mauricio.async.db.postgresql.messages.backend

class ReadyForQueryMessage(transactionStatus: Char) : ServerMessage(ServerMessage.ReadyForQuery)