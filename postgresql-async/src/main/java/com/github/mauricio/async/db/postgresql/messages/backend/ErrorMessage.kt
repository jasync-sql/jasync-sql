package com.github.mauricio.async.db.postgresql.messages.backend

class ErrorMessage(fields: Map<Char, String>) : InformationMessage(ServerMessage.Error, fields)
