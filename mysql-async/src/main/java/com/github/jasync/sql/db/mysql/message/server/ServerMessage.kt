package com.github.jasync.sql.db.mysql.message.server

import com.github.jasync.sql.db.KindedMessage


abstract class ServerMessage(override val kind: Int) : KindedMessage {

    companion object {
        const val ServerProtocolVersion = 10
        const val Error = -1
        const val Ok = 0
        const val EOF = -2

        // these messages don't actually exist
        // but we use them to simplify the switch statements
        const val ColumnDefinition = 100
        const val ColumnDefinitionFinished = 101
        const val ParamProcessingFinished = 102
        const val ParamAndColumnProcessingFinished = 103
        const val Row = 104
        const val BinaryRow = 105
        const val PreparedStatementPrepareResponse = 106
    }
}
