package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.NoticeMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import java.nio.charset.Charset

class NoticeParser(charset: Charset) : InformationParser(charset) {
  override fun createMessage(fields: Map<Char, String>): ServerMessage = NoticeMessage(fields)
}
