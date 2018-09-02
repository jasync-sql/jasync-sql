package com.github.mauricio.async.db.postgresql.messages.backend

class NoticeMessage(fields: Map<Char, String>) : InformationMessage(ServerMessage.Notice, fields)