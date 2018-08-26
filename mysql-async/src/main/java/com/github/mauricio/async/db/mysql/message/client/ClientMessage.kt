
package com.github.mauricio.async.db.mysql.message.client

import com.github.jasync.sql.db.KindedMessage



class ClientMessage (override val kind : Int ) : KindedMessage {
  companion object {
    val ClientProtocolVersion = 0x09 // COM_STATISTICS
    val Quit = 0x01 // COM_QUIT
    val Query = 0x03 // COM_QUERY
    val PreparedStatementPrepare = 0x16 // COM_STMT_PREPARE
    val PreparedStatementExecute = 0x17 // COM_STMT_EXECUTE
    val PreparedStatementSendLongData = 0x18 // COM_STMT_SEND_LONG_DATA
    val AuthSwitchResponse = 0xfe // AuthSwitchRequest
  }
}
