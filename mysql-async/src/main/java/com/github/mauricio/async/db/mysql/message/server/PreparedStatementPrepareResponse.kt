
package com.github.mauricio.async.db.mysql.message.server

data class PreparedStatementPrepareResponse (
    val statementId : ByteArray,
    val warningCount : Short,
    val paramsCount : Int,
    val columnsCount : Int )
  : ServerMessage( ServerMessage.PreparedStatementPrepareResponse )
