
package com.github.jasync.sql.db.mysql.message.server

data class EOFMessage( val warningCount : Int, val flags : Int  )
  : ServerMessage( ServerMessage.EOF )
