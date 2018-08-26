
package com.github.mauricio.async.db.mysql.message.server

data class ParamProcessingFinishedMessage( val eofMessage : EOFMessage )
  : ServerMessage( ServerMessage.ParamProcessingFinished )
