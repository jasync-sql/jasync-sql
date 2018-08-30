package com.github.jasync.sql.db.util

import junit.framework.TestCase.assertEquals
import org.junit.Test

class HexCodecSpec {
    val sampleArray = byteArrayOf(83, 97, 121, 32, 72, 101, 108, 108, 111, 32, 116, 111, 32, 77, 121, 32, 76, 105, 116, 116, 108, 101, 32, 70, 114, 105, 101, 110, 100)
    val sampleHex = "5361792048656c6c6f20746f204d79204c6974746c6520467269656e64".toUpperCase()
    val HexStart = "\\x"
    val HexStartChars = HexStart.toCharArray()

    @Test
    fun `correctly generate an array of bytes`() {
        val bytes: ByteArray = HexCodec.decode("5361792048656c6c6f20746f204d79204c6974746c6520467269656e64", 0)
        assertEquals(bytes.toList(), sampleArray.toList())
    }

    @Test
    fun `correctly generate a string from an array of bytes`() {
        assertEquals(HexCodec.encode(sampleArray, charArrayOf()), sampleHex)
    }

    @Test
    fun `correctly generate a byte array from the PG output`() {

        val input = "\\x53617920"
        val bytes = byteArrayOf(83, 97, 121, 32)
        assertEquals(HexCodec.decode(input, 2).toList(), bytes.toList())
    }

    @Test
    fun `correctly encode to hex using the PostgreSQL format`() {
        HexCodec.encode(sampleArray, HexStartChars) === (HexStart + sampleHex)
    }
}
