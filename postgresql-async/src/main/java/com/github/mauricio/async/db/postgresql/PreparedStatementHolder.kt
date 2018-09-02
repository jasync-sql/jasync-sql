package com.github.mauricio.async.db.postgresql

import com.github.mauricio.async.db.postgresql.messages.backend.PostgreSQLColumnData

class PreparedStatementHolder(val query: String, val statementId: Int) {

  val pair = {
    var result = StringBuilder(query.length + 16)
    var offset = 0
    var params = 0
    while (offset < query.length) {
      val next = query.indexOf('?', offset)
      if (next == -1) {
        result++ = query.substring(offset)
        offset = query.length
      } else {
        result++ = query.substring(offset, next)
        offset = next + 1
        if (offset < query.length && query[offset] == '?') {
          result += '?'
          offset += 1
        } else {
          result += '$'
          params += 1
          result++ = params.toString()
        }
      }
    }
    Pair(result.toString(), params)
  }

  var prepared: Boolean = false
  var columnDatas: Array<PostgreSQLColumnData> = emptyArray()

}