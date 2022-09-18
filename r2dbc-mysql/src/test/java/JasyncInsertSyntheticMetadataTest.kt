package com.github.jasync.r2dbc.mysql

import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

/**
 * Unit tests for [JasyncInsertSyntheticMetadata].
 */
class JasyncInsertSyntheticMetadataTest {

    private val metadata1 = JasyncInsertSyntheticMetadata("SomeId")

    private val metadata2 = JasyncInsertSyntheticMetadata("GoodsId")

    @Test
    fun getColumnMetadatas() {
        assertEquals(metadata1.columnMetadatas, mutableListOf(metadata1))
        assertEquals(metadata2.columnMetadatas, mutableListOf(metadata2))
    }

    @Test
    fun getColumnMetadata() {
        assertEquals(metadata1.getColumnMetadata(0), metadata1)
        assertEquals(metadata1.getColumnMetadata("SomeId"), metadata1)
        assertEquals(metadata1.getColumnMetadata("someId"), metadata1)
        assertEquals(metadata1.getColumnMetadata("SOMEID"), metadata1)

        assertEquals(metadata2.getColumnMetadata(0), metadata2)
        assertEquals(metadata2.getColumnMetadata("GoodsId"), metadata2)
        assertEquals(metadata2.getColumnMetadata("goodsId"), metadata2)
        assertEquals(metadata2.getColumnMetadata("GOODSID"), metadata2)
    }

    @Test
    fun getColumnNames() {
        assertEquals(metadata1.columnNames, setOf("SomeId"))
        assertEquals(metadata2.columnNames, setOf("GoodsId"))

        assertTrue("SomeId" in metadata1.columnNames)
        assertTrue("someId" in metadata1.columnNames)
        assertTrue("SOMEID" in metadata1.columnNames)
        assertTrue("GoodsId" in metadata2.columnNames)
        assertTrue("goodsId" in metadata2.columnNames)
        assertTrue("GOODSID" in metadata2.columnNames)
    }

    @Test
    fun getName() {
        assertEquals(metadata1.name, "SomeId")
        assertEquals(metadata2.name, "GoodsId")
    }
}
