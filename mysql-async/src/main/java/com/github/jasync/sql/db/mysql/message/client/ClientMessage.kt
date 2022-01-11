package com.github.jasync.sql.db.mysql.message.client

import com.github.jasync.sql.db.KindedMessage

open class ClientMessage(override val kind: Int) : KindedMessage {
    companion object {
        const val ClientProtocolVersion = 0x09 // COM_STATISTICS
        const val Quit = 0x01 // COM_QUIT
        const val Query = 0x03 // COM_QUERY
        const val PreparedStatementPrepare = 0x16 // COM_STMT_PREPARE
        const val PreparedStatementExecute = 0x17 // COM_STMT_EXECUTE
        const val PreparedStatementSendLongData = 0x18 // COM_STMT_SEND_LONG_DATA
        const val PreparedStatementClose = 0x19 // COM_STMT_CLOSE
        const val SslRequest = 0xfd // SSLRequest
        const val AuthSwitchResponse = 0xfe // AuthSwitchRequest
    }
}
