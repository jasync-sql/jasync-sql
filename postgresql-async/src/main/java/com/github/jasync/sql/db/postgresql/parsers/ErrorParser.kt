package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.messages.backend.ErrorMessage
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import java.nio.charset.Charset


class ErrorParser(charset: Charset) : InformationParser(charset) {

    override fun createMessage(fields: Map<Char, String>): ServerMessage = ErrorMessage(fields)

}
