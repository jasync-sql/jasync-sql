package com.github.aysnc.sql.db.integration

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PostgisSpec : DatabaseTestHelper() {

    @Test
    fun `simple query`() {

        withHandler { handler ->
            val res1 = executeQuery(handler, "SELECT postgis_full_version()")
            assertThat(res1.rows[0][0].toString()).contains("POSTGIS=")
            val res2 = executeQuery(handler, "SELECT ST_GeomFromText('POINT(1 2)',4326)")
//            val res2 = executeQuery(handler, "SELECT ST_GeomFromText('LINESTRING(1 2, 3 4)',4326)")
            // can try to parse it similar to https://github.com/postgis/postgis-java/blob/main/postgis-jdbc-geometry/src/main/java/net/postgis/jdbc/geometry/binary/BinaryParser.java
            assertThat(bytesToHex(res2.rows[0][0] as ByteArray)).contains("POSTGIS=")
        }
    }

    private val HEX_ARRAY = "0123456789ABCDEF".toCharArray()
    fun bytesToHex(bytes: ByteArray): String {
        val hexChars = CharArray(bytes.size * 2)
        for (j in bytes.indices) {
            val v = bytes[j].toInt() and 0xFF
            hexChars[j * 2] = HEX_ARRAY[v ushr 4]
            hexChars[j * 2 + 1] = HEX_ARRAY[v and 0x0F]
        }
        return String(hexChars)
    }
}
