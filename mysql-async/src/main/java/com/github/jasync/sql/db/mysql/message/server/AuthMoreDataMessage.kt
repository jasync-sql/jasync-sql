package com.github.jasync.sql.db.mysql.message.server

data class AuthMoreDataMessage(
    val data: Byte
) : ServerMessage(AuthMoreData) {
    fun isSuccess(): Boolean {
        return data == 3.toByte()
    }
}
