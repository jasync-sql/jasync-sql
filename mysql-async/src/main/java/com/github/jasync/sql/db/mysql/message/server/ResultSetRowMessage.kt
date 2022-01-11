package com.github.jasync.sql.db.mysql.message.server

import io.netty.buffer.ByteBuf

class ResultSetRowMessage(private val buffer: MutableList<ByteBuf?> = mutableListOf()) :
    ServerMessage(ServerMessage.Row), MutableList<ByteBuf?> by buffer
