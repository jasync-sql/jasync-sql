package com.github.jasync.sql.db.mysql.message.client

import io.netty.buffer.ByteBuf

data class SendLongDataMessage(
    @Suppress("ArrayInDataClass")
    val statementId: ByteArray,
    val value: ByteBuf,
    val paramId: Int
)
