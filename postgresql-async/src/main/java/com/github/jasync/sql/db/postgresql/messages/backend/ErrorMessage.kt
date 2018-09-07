package com.github.jasync.sql.db.postgresql.messages.backend

class ErrorMessage(fields: Map<Char, String>) : InformationMessage(ServerMessage.Error, fields)
