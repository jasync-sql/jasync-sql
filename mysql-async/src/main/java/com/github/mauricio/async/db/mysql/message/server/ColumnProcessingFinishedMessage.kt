
package com.github.mauricio.async.db.mysql.message.server

data class ColumnProcessingFinishedMessage( val eofMessage : EOFMessage ) : ServerMessage( ServerMessage.ColumnDefinitionFinished )
