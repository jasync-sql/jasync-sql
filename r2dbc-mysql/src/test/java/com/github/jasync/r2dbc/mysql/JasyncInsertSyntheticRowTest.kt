package com.github.jasync.r2dbc.mysql

import java.math.BigInteger
import kotlin.test.assertEquals
import org.junit.Test

/**
 * Unit tests for [JasyncInsertSyntheticRow].
 */
internal class JasyncInsertSyntheticRowTest {

    @Test
    @ExperimentalUnsignedTypes
    fun get() {
        val positive = JasyncInsertSyntheticRow("SomeId", 1)
        val overflowed = JasyncInsertSyntheticRow("GoodsId", ULong.MAX_VALUE.toLong())

        assertEquals(positive["SomeId"]?.javaClass as Class<*>?, java.lang.Long::class.java)
        assertEquals(positive["SomeId", Any::class.java]?.javaClass as Class<*>?, java.lang.Long::class.java)
        assertEquals(positive["SomeId"], 1L)
        assertEquals(positive["someId"], 1L)
        assertEquals(positive["SOMEID"], 1L)
        assertEquals(positive[0], 1L)
        assertEquals(positive["SomeId", java.lang.Integer::class.java] as Int, 1)
        assertEquals(positive["SomeId", java.lang.Number::class.java] as Long, 1L)
        assertEquals(positive[0, java.lang.Integer::class.java] as Int, 1)
        assertEquals(positive[0, java.lang.Number::class.java] as Long, 1L)

        assertEquals(overflowed["GoodsId"]?.javaClass as Class<*>?, BigInteger::class.java)
        assertEquals(overflowed["GoodsId", Any::class.java]?.javaClass as Class<*>?, BigInteger::class.java)
        assertEquals(overflowed["GoodsId"], BigInteger(ULong.MAX_VALUE.toString()))
        assertEquals(overflowed["goodsId"], BigInteger(ULong.MAX_VALUE.toString()))
        assertEquals(overflowed["GOODSID"], BigInteger(ULong.MAX_VALUE.toString()))
        assertEquals(overflowed[0], BigInteger(ULong.MAX_VALUE.toString()))
        assertEquals(overflowed["GoodsId", java.lang.Long::class.java] as Long, -1L)
        assertEquals(overflowed["GoodsId", java.lang.Number::class.java] as BigInteger, BigInteger(ULong.MAX_VALUE.toString()))
        assertEquals(overflowed[0, java.lang.Long::class.java] as Long, -1L)
        assertEquals(overflowed[0, java.lang.Number::class.java] as BigInteger, BigInteger(ULong.MAX_VALUE.toString()))
    }
}
