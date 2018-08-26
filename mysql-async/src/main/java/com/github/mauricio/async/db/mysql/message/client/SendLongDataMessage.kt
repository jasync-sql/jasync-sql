package com.github.mauricio.async.db.mysql.message.client

import io.netty.buffer.ByteBuf

data class SendLongDataMessage(
    val statementId: ByteArray,
    val value: ByteBuf,
    val paramId: Int
)
