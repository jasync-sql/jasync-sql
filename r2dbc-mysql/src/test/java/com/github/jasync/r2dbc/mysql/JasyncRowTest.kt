package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.RowData
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [JasyncRow].
 */
internal class JasyncRowTest {

    @Test
    fun testBooleanConversion() {
        // Mock dependencies
        val rowData = mockk<RowData>()
        val metadata = mockk<JasyncMetadata>()

        // Setup mock values for different types of data
        every { rowData["boolTrue"] } returns true
        every { rowData["boolFalse"] } returns false
        every { rowData["numZero"] } returns 0
        every { rowData["numOne"] } returns 1
        every { rowData["stringTrue"] } returns "true"
        every { rowData["stringFalse"] } returns "false"
        every { rowData[0] } returns true
        every { rowData[1] } returns 0
        every { rowData[2] } returns "true"

        val row = JasyncRow(rowData, metadata)

        // Test conversion from Boolean to Boolean
        assertTrue(row.get("boolTrue", java.lang.Boolean::class.java) as Boolean)
        assertFalse(row.get("boolFalse", java.lang.Boolean::class.java) as Boolean)

        // Test conversion from Number to Boolean
        assertFalse(row.get("numZero", java.lang.Boolean::class.java) as Boolean)
        assertTrue(row.get("numOne", java.lang.Boolean::class.java) as Boolean)

        // Test conversion from String to Boolean
        assertTrue(row.get("stringTrue", java.lang.Boolean::class.java) as Boolean)
        assertFalse(row.get("stringFalse", java.lang.Boolean::class.java) as Boolean)

        // Test conversion from various types using index
        assertTrue(row.get(0, java.lang.Boolean::class.java) as Boolean)
        assertFalse(row.get(1, java.lang.Boolean::class.java) as Boolean)
        assertTrue(row.get(2, java.lang.Boolean::class.java) as Boolean)

        // Test conversion to primitive boolean - using Boolean class in Kotlin
        assertTrue(row.get("boolTrue", java.lang.Boolean::class.java) as Boolean)
        assertFalse(row.get("boolFalse", java.lang.Boolean::class.java) as Boolean)
    }
}
