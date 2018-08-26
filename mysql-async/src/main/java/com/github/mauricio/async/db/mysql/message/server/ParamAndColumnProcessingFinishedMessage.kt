
package com.github.mauricio.async.db.mysql.message.server

data class ParamAndColumnProcessingFinishedMessage ( val eofMessage : EOFMessage )
  : ServerMessage( ServerMessage.ParamAndColumnProcessingFinished  )
