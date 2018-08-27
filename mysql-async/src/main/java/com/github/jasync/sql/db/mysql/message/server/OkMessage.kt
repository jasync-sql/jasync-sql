
package com.github.jasync.sql.db.mysql.message.server

data class OkMessage(
    val affectedRows : Long,
    val lastInsertId : Long,
    val statusFlags : Int,
    val warnings : Int,
    val message : String )
  : ServerMessage( ServerMessage.Ok )
