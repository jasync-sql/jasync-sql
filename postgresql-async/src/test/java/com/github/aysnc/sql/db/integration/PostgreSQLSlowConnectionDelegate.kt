package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.postgresql.codec.PostgreSQLConnectionDelegate

class PostgreSQLSlowConnectionDelegate(
    private val delegate: PostgreSQLConnectionDelegate,
    private val onReadyForQuerySlowdownInMillis: Int
) : PostgreSQLConnectionDelegate by delegate {
    override fun onReadyForQuery() {
        Thread.sleep(onReadyForQuerySlowdownInMillis.toLong())
        delegate.onReadyForQuery()
    }
}
