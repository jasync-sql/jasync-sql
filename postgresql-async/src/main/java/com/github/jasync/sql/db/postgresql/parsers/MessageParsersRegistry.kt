package com.github.jasync.sql.db.postgresql.parsers

import com.github.jasync.sql.db.exceptions.ParserNotAvailableException
import com.github.jasync.sql.db.postgresql.messages.backend.ServerMessage
import io.netty.buffer.ByteBuf
import java.nio.charset.Charset


class MessageParsersRegistry(val charset: Charset) {
    private val commandCompleteParser = CommandCompleteParser(charset)
    private val errorParser = ErrorParser(charset)
    private val noticeParser = NoticeParser(charset)
    private val parameterStatusParser = ParameterStatusParser(charset)
    private val rowDescriptionParser = RowDescriptionParser(charset)
    private val notificationResponseParser = NotificationResponseParser(charset)

    private fun parserFor(t: Int): MessageParser {
        return when (t) {
            ServerMessage.Authentication -> AuthenticationStartupParser
            ServerMessage.BackendKeyData -> BackendKeyDataParser
            ServerMessage.BindComplete -> ReturningMessageParser.BindCompleteMessageParser
            ServerMessage.CloseComplete -> ReturningMessageParser.CloseCompleteMessageParser
            ServerMessage.CommandComplete -> this.commandCompleteParser
            ServerMessage.DataRow -> DataRowParser
            ServerMessage.Error -> this.errorParser
            ServerMessage.EmptyQueryString -> ReturningMessageParser.EmptyQueryStringMessageParser
            ServerMessage.NoData -> ReturningMessageParser.NoDataMessageParser
            ServerMessage.Notice -> this.noticeParser
            ServerMessage.NotificationResponse -> this.notificationResponseParser
            ServerMessage.ParameterStatus -> this.parameterStatusParser
            ServerMessage.ParseComplete -> ReturningMessageParser.ParseCompleteMessageParser
            ServerMessage.RowDescription -> this.rowDescriptionParser
            ServerMessage.ReadyForQuery -> ReadyForQueryParser
            else -> throw  ParserNotAvailableException(t.toByte())
        }
    }

    fun parse(t: Int, b: ByteBuf): ServerMessage = parserFor(t).parseMessage(b)

}
