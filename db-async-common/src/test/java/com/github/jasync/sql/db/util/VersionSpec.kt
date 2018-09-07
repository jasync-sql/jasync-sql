package com.github.jasync.sql.db.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionSpec {

    @Test
    fun `correctly parse versions`() {
        val version = parseVersion("9.1.4")
        assertEquals(Version(9, 1, 4), version)
        assertEquals(9, version.major)
        assertEquals(1, version.minor)
        assertEquals(4, version.patch)
    }

    @Test
    fun `correctly parse with missing fields`() {
        val version = parseVersion("8.7")
        assertEquals(Version(8, 7, 0), version)
    }

    @Test
    fun `correctly compare between major different versions 1`() {

        val version1 = parseVersion("8.2.0")
        val version2 = parseVersion("9.2.0")

        assertTrue(version2 > version1)

    }

    @Test
    fun `correctly compare between major different versions 2`() {

        val version1 = parseVersion("8.2.0")
        val version2 = parseVersion("8.2.0")

        assertEquals(version2, version1)

    }


    @Test
    fun `correctly compare between major different versions 3`() {

        val version1 = parseVersion("8.2.8")
        val version2 = parseVersion("8.2.87")

        assertTrue(version2 > version1)

    }

    @Test
    fun `correctly compare two different versions`() {

        val version1 = parseVersion("9.1.2")
        val version2 = parseVersion("9.2.0")

        assertTrue(version2 > version1)

    }

}


