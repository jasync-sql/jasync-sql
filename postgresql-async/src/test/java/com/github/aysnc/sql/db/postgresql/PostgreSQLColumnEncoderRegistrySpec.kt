package com.github.aysnc.sql.db.postgresql

import com.github.jasync.sql.db.postgresql.column.PostgreSQLColumnEncoderRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PostgreSQLColumnEncoderRegistrySpec {

    val encoder = PostgreSQLColumnEncoderRegistry.Instance

    @Test
    fun `column encoder registry should encode Some(value) like value`() {
        val actual = encoder.encode(1L)
        val expected = encoder.encode(1L)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `column encoder registry should encode None as null`() {
        val actual = encoder.encode(null)
        val expected = encoder.encode(null)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `column encoder registry should determine kindOf Some(value) like kindOf value`() {
        val actual = encoder.kindOf(1L)
        val expected = encoder.kindOf(1L)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `column encoder registry should determine kindOf None like kindOf null`() {
        val actual = encoder.kindOf(null)
        val expected = encoder.kindOf(null)

        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `column encoder registry should encodes Some(null) as null`() {
        val actual = encoder.encode(null)
        assertThat(actual).isEqualTo(null)
    }

    @Test
    fun `column encoder registry should encodes null as null`() {
        val actual = encoder.encode(null)
        assertThat(actual).isEqualTo(null)
    }
}
