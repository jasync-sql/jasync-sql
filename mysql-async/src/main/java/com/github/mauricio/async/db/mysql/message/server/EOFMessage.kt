
package com.github.mauricio.async.db.mysql.message.server

data class EOFMessage( val warningCount : Int, val flags : Int  )
  : ServerMessage( ServerMessage.EOF )
