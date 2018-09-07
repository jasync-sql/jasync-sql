package com.github.jasync.sql.db.postgresql.messages.backend

class ReadyForQueryMessage(transactionStatus: Char) : ServerMessage(ServerMessage.ReadyForQuery)
