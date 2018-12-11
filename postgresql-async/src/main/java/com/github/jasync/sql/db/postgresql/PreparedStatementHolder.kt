package com.github.jasync.sql.db.postgresql

import com.github.jasync.sql.db.postgresql.messages.backend.PostgreSQLColumnData

class PreparedStatementHolder(val query: String, val statementId: Int) {

    val realQuery: String get() = pair.first
    val paramsCount: Int get() = pair.second

    val pair: Pair<String, Int> = {
        val result = StringBuilder(query.length + 16)
        var offset = 0
        var params = 0
        while (offset < query.length) {
            val next = query.indexOf('?', offset)
            if (next == -1) {
                result.append(query.substring(offset))
                offset = query.length
            } else {
                result.append(query.substring(offset, next))
                offset = next + 1
                if (offset < query.length && query[offset] == '?') {
                    result.append('?')
                    offset += 1
                } else {
                    result.append('$')
                    params += 1
                    result.append(params.toString())
                }
            }
        }
        Pair(result.toString(), params)
    }()

    var prepared: Boolean = false
    var columnDatas: List<PostgreSQLColumnData> = emptyList()

}
