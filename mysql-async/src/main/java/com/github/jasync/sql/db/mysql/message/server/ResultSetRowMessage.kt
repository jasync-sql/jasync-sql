package com.github.jasync.sql.db.mysql.message.server

import com.github.jasync.sql.db.util.length
import io.netty.buffer.ByteBuf

class ResultSetRowMessage(private val buffer: MutableList<ByteBuf?> = mutableListOf()) :
    ServerMessage(ServerMessage.Row)
    , List<ByteBuf?> by buffer {

    fun length(): Int = buffer.length

    fun add(elem: ByteBuf?): ResultSetRowMessage {
        this.buffer.add(elem)
        return this
    }


}
