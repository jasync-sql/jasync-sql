package com.github.jasync.sql.db.mysql.message.server

import com.github.jasync.sql.db.KindedMessage


abstract class ServerMessage(override val kind: Int) : KindedMessage {

    companion object {
        val ServerProtocolVersion = 10
        val Error = -1
        val Ok = 0
        val EOF = -2

        // these messages don't actually exist
        // but we use them to simplify the switch statements
        val ColumnDefinition = 100
        val ColumnDefinitionFinished = 101
        val ParamProcessingFinished = 102
        val ParamAndColumnProcessingFinished = 103
        val Row = 104
        val BinaryRow = 105
        val PreparedStatementPrepareResponse = 106
    }
}
