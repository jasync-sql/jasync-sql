package com.github.jasync.sql.db

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.LocalDateTime

class RowDataTest {


    @Test
    fun `check get Int`() {
        val value = 5
        val tested = ForTestingRowData(value)
        assertThat(tested.getInt(0)).isEqualTo(value)
        assertThat(tested.getInt("0")).isEqualTo(value)
        assertThat(tested.getInt(1)).isNull()
        assertThat(tested.getInt("1")).isNull()
    }

    @Test
    fun `check get Long`() {
        val value = 5L
        val tested = ForTestingRowData(value)
        assertThat(tested.getLong(0)).isEqualTo(value)
        assertThat(tested.getLong("0")).isEqualTo(value)
    }

    @Test
    fun `check get Boolean`() {
        val value = true
        val tested = ForTestingRowData(value)
        assertThat(tested.getBoolean(0)).isEqualTo(value)
        assertThat(tested.getBoolean("0")).isEqualTo(value)
    }

    @Test
    fun `check get Boolean for byte`() {
        val value = 1.toByte()
        val tested = ForTestingRowData(value)
        assertThat(tested.getBoolean(0)).isEqualTo(true)
        assertThat(tested.getBoolean("0")).isEqualTo(true)
    }

    @Test
    fun `check get Boolean for byte array`() {
        val value = byteArrayOf(1.toByte())
        val tested = ForTestingRowData(value)
        assertThat(tested.getBoolean(0)).isEqualTo(true)
        assertThat(tested.getBoolean("0")).isEqualTo(true)
    }

    @Test
    fun `check get Byte`() {
        val value = 1.toByte()
        val tested = ForTestingRowData(value)
        assertThat(tested.getByte(0)).isEqualTo(value)
        assertThat(tested.getByte("0")).isEqualTo(value)
    }

    @Test
    fun `check get date`() {
        val value = LocalDateTime.now()
        val tested = ForTestingRowData(value)
        assertThat(tested.getDate(0)).isEqualTo(value)
        assertThat(tested.getDate("0")).isEqualTo(value)
    }

    @Test
    fun `check get float`() {
        val value = 0.1f
        val tested = ForTestingRowData(value)
        assertThat(tested.getFloat(0)).isEqualTo(value)
        assertThat(tested.getFloat("0")).isEqualTo(value)
    }

    @Test
    fun `check get double`() {
        val value = 0.1
        val tested = ForTestingRowData(value)
        assertThat(tested.getDouble(0)).isEqualTo(value)
        assertThat(tested.getDouble("0")).isEqualTo(value)
    }

    @Test
    fun `check get as custome type`() {
        val value = "a" to "b"
        val tested = ForTestingRowData(value)
        val pair1: Pair<String, String> = tested.getAs<Pair<String, String>>(0)
        assertThat(pair1).isEqualTo(value)
        val pair2: Pair<String, String> = tested.getAs<Pair<String, String>>("0")
        assertThat(pair2).isEqualTo(value)
        val pair3: Pair<String, String>? = tested.getAs<Pair<String, String>?>(1)
        assertThat(pair3).isEqualTo(null)
        val pair4: Pair<String, String>? = tested.getAs<Pair<String, String>?>("1")
        assertThat(pair4).isEqualTo(null)
    }

    @Test
    fun `check get string`() {
        val value = "Hi"
        val tested = ForTestingRowData(value)
        assertThat(tested.getString(0)).isEqualTo(value)
        assertThat(tested.getString("0")).isEqualTo(value)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `check exception`() {
        val value = 5
        val tested = ForTestingRowData(value)
        assertThat(tested.getInt(2)).isEqualTo(value)
    }

    @Test(expected = UnsupportedOperationException::class)
    fun `check exception name method`() {
        val value = 5
        val tested = ForTestingRowData(value)
        assertThat(tested.getInt("2")).isEqualTo(value)
    }

}

class ForTestingRowData(val value: Any?) : RowData, List<Any?> by listOf(value) {
    override fun get(index: Int): Any? = when (index) {
        0 -> value
        1 -> null
        else -> throw UnsupportedOperationException()
    }

    override fun get(column: String): Any? = when (column) {
        "0" -> value
        "1" -> null
        else -> throw UnsupportedOperationException()
    }

    override fun rowNumber(): Int = 0

}
