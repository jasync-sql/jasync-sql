package com.github.jasync.sql.db.mysql.codec

import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.mysql.message.server.AuthMoreDataMessage
import com.github.jasync.sql.db.mysql.message.server.AuthenticationSwitchRequest
import com.github.jasync.sql.db.mysql.message.server.EOFMessage
import com.github.jasync.sql.db.mysql.message.server.ErrorMessage
import com.github.jasync.sql.db.mysql.message.server.HandshakeMessage
import com.github.jasync.sql.db.mysql.message.server.OkMessage
import io.netty.channel.ChannelHandlerContext

interface MySQLHandlerDelegate {

    fun onHandshake(message: HandshakeMessage)
    fun onError(message: ErrorMessage)
    fun onOk(message: OkMessage)
    fun onEOF(message: EOFMessage)
    fun exceptionCaught(exception: Throwable)
    fun connected(ctx: ChannelHandlerContext)
    fun onResultSet(resultSet: ResultSet, message: EOFMessage)
    fun switchAuthentication(message: AuthenticationSwitchRequest)
    fun onAuthMoreData(message: AuthMoreDataMessage)
    fun unregistered()
}
