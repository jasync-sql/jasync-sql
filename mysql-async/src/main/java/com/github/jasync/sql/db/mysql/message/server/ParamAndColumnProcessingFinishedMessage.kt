
package com.github.jasync.sql.db.mysql.message.server

data class ParamAndColumnProcessingFinishedMessage ( val eofMessage : EOFMessage )
  : ServerMessage( ServerMessage.ParamAndColumnProcessingFinished  )
