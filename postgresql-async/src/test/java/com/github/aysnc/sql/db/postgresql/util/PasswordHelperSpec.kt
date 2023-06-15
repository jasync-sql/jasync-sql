package com.github.aysnc.sql.db.postgresql.util

import com.github.jasync.sql.db.postgresql.util.PasswordHelper
import com.github.jasync.sql.db.util.length
import io.netty.util.CharsetUtil
import org.junit.Test

class PasswordHelperSpec {

    val salt = byteArrayOf(-31, 68, 99, 36)
    val result = byteArrayOf(
        109,
        100,
        53,
        54,
        102,
        57,
        55,
        57,
        98,
        99,
        51,
        101,
        100,
        100,
        54,
        101,
        56,
        52,
        57,
        49,
        100,
        52,
        101,
        99,
        49,
        55,
        100,
        57,
        97,
        51,
        102,
        97,
        97,
        55,
        56
    )

    fun printArray(name: String, bytes: ByteArray) {
        println(String.format("%s %s -> (%s)%n", name, bytes.length, bytes.joinToString(",")))
    }

    @Test
    fun `helper should generate the same value as the PostgreSQL code`() {
        val username = "jasync"
        val password = "example"

        PasswordHelper.encode(username, password, salt, CharsetUtil.UTF_8) === result
    }
}
