package com.github.jasync.sql.db

import com.github.jasync.sql.db.mysql.codec.MySQLHandlerDelegate
import com.github.jasync.sql.db.mysql.message.server.OkMessage

class MySQLSlowConnectionDelegate(
    private val delegate: MySQLHandlerDelegate,
    private val onOkSlowdownInMillis: Int
) : MySQLHandlerDelegate by delegate {
    override fun onOk(message: OkMessage) {
        Thread.sleep(onOkSlowdownInMillis.toLong())
        delegate.onOk(message)
    }
}
