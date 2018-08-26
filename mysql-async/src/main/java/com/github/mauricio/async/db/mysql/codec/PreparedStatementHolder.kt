
package com.github.mauricio.async.db.mysql.codec

import com.github.jasync.sql.db.util.length
import com.github.mauricio.async.db.mysql.message.server.ColumnDefinitionMessage
import com.github.mauricio.async.db.mysql.message.server.PreparedStatementPrepareResponse

class PreparedStatementHolder( val statement : String, val message : PreparedStatementPrepareResponse ) {

  val columns = mutableListOf<ColumnDefinitionMessage>()
  val parameters = mutableListOf<ColumnDefinitionMessage>()

  fun statementId (): ByteArray = message.statementId

  fun needsParameters (): Boolean = message.paramsCount != this.parameters.length

  fun needsColumns (): Boolean = message.columnsCount != this.columns.length

  fun needsAny (): Boolean = this.needsParameters() || this.needsColumns()

  fun add( column : ColumnDefinitionMessage ) {
    if ( this.needsParameters() ) {
      this.parameters += column
    } else {
      if ( this.needsColumns() ) {
        this.columns += column
      }
    }
  }

  override fun toString(): String = "PreparedStatementHolder($statement)"

}
