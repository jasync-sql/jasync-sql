package com.github.jasync.sql.db.postgresql.messages.backend

class NoticeMessage(fields: Map<Char, String>) : InformationMessage(ServerMessage.Notice, fields)
