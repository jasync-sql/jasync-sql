package com.github.jasync.sql.db.mysql.message.server

import io.netty.buffer.ByteBuf

data class BinaryRowMessage(val buffer: ByteBuf) : ServerMessage(ServerMessage.BinaryRow)
