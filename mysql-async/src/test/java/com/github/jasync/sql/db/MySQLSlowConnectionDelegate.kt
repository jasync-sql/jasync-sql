package com.github.jasync.sql.db

import com.github.jasync.sql.db.mysql.codec.MySQLHandlerDelegate
import com.github.jasync.sql.db.mysql.message.server.AuthenticationSwitchRequest
import com.github.jasync.sql.db.mysql.message.server.HandshakeMessage
import com.github.jasync.sql.db.mysql.message.server.EOFMessage
import com.github.jasync.sql.db.mysql.message.server.ErrorMessage
import com.github.jasync.sql.db.mysql.message.server.OkMessage
import io.netty.channel.ChannelHandlerContext

class MySQLSlowConnectionDelegate(
    private val delegate: MySQLHandlerDelegate,
    private val onOkSlowdownInMillis: Long
) : MySQLHandlerDelegate {
    override fun onHandshake(message: HandshakeMessage) =
        delegate.onHandshake(message)

    override fun onError(message: ErrorMessage) =
        delegate.onError(message)

    override fun onOk(message: OkMessage) {
        Thread.sleep(onOkSlowdownInMillis)
        delegate.onOk(message)
    }

    override fun onEOF(message: EOFMessage) =
        delegate.onEOF(message)

    override fun exceptionCaught(exception: Throwable) =
        delegate.exceptionCaught(exception)

    override fun connected(ctx: ChannelHandlerContext) =
        delegate.connected(ctx)

    override fun onResultSet(resultSet: ResultSet, message: EOFMessage) =
        delegate.onResultSet(resultSet, message)

    override fun switchAuthentication(message: AuthenticationSwitchRequest) =
        delegate.switchAuthentication(message)

    override fun unregistered() =
        delegate.unregistered()
}
