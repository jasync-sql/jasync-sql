package com.github.mauricio.async.db.postgresql.messages.backend

import java.util.*

data class RowDescriptionMessage(val columnDatas: Array<PostgreSQLColumnData>) : ServerMessage(ServerMessage.RowDescription) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as RowDescriptionMessage

    if (!Arrays.equals(columnDatas, other.columnDatas)) return false

    return true
  }

  override fun hashCode(): Int {
    return Arrays.hashCode(columnDatas)
  }
}