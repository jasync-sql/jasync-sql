package com.github.aysnc.sql.db.postgresql.column

import com.github.jasync.sql.db.postgresql.column.PostgreSQLColumnEncoderRegistry
import org.junit.Test

class DefaultColumnEncoderRegistrySpec {

    val registry = PostgreSQLColumnEncoderRegistry()

    @Test
    fun `"registry" should"correctly render an array of strings , nulls"`() {
        val items = arrayOf("some", """text \ hoes " here to be seen""", null, "all, right")
        registry.encode(items) === """{"some","text \\ hoes \" here to be seen",NULL,"all, right"}"""
    }

    @Test
    fun `"registry" should"correctly render an array of numbers"`() {
        val items = arrayOf(arrayOf(1, 2, 3), arrayOf(4, 5, 6), arrayOf(7, null, 8))
        registry.encode(items) === "{{1,2,3},{4,5,6},{7,NULL,8}}"
    }
}
