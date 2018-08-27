
package com.github.jasync.sql.db.mysql.message.server

data class ColumnProcessingFinishedMessage( val eofMessage : EOFMessage ) : ServerMessage( ServerMessage.ColumnDefinitionFinished )
