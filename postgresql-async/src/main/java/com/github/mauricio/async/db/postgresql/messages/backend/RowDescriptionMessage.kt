package com.github.mauricio.async.db.postgresql.messages.backend

data class RowDescriptionMessage(val columnDatas: List<PostgreSQLColumnData>) : ServerMessage(ServerMessage.RowDescription) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RowDescriptionMessage

    return columnDatas == other.columnDatas
  }

  override fun hashCode(): Int {
    return columnDatas.hashCode()
  }
}
