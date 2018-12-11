package com.github.jasync.sql.db.postgresql.messages.backend

import io.netty.buffer.ByteBuf
import java.util.*

data class DataRowMessage(val values: Array<ByteBuf?>) : ServerMessage(ServerMessage.DataRow) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DataRowMessage

        if (!Arrays.equals(values, other.values)) return false

        return true
    }

    override fun hashCode(): Int {
        return Arrays.hashCode(values)
    }
}
