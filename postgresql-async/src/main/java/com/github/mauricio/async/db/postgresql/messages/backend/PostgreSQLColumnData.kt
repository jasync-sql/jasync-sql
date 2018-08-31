package com.github.mauricio.async.db.postgresql.messages.backend

import com.github.jasync.sql.db.general.ColumnData

data class PostgreSQLColumnData(
    override val name: String,
    val tableObjectId: Int,
    val columnNumber: Int,
    val dataType: Int,
    val dataTypeSize: Long,
    val dataTypeModifier: Int,
    val fieldFormat: Int) : ColumnData {

  override fun dataType(): Int = this.dataType
  override fun dataTypeSize(): Long = this.dataTypeSize

  override fun toString(): String {
    return "PostgreSQLColumnData(name='$name', tableObjectId=$tableObjectId, columnNumber=$columnNumber, dataType=$dataType, dataTypeSize=$dataTypeSize, dataTypeModifier=$dataTypeModifier, fieldFormat=$fieldFormat)"
  }

}