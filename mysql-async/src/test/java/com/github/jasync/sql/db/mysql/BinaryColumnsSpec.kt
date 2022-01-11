package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.RowData
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import java.nio.ByteBuffer
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.Assert.assertArrayEquals
import org.junit.Test

class BinaryColumnsSpec : ConnectionHelper() {

    @Test
    fun `correctly load fields as byte arrays`() {

        val create = """CREATE TEMPORARY TABLE t (
                       id BIGINT NOT NULL AUTO_INCREMENT,
                       uuid BINARY(36) NOT NULL,
                       address VARBINARY(16) NOT NULL,
                       PRIMARY KEY (id),
                       INDEX idx_t_uuid (uuid),
                       INDEX idx_t_address (address)
                     );"""

        val uuid = UUID.randomUUID().toString()
        val host = "127.0.0.1"

        val preparedInsert = "INSERT INTO t (uuid, address) VALUES (?, ?)"
        val insert = "INSERT INTO t (uuid, address) VALUES ('$uuid', '$host')"
        val select = "SELECT * FROM t"

        withConnection { connection ->
            executeQuery(connection, create)
            executeQuery(connection, insert)

            val result = assertNotNull(executeQuery(connection, select).rows)
            val b = assertNotNull(result[0])
            compareBytes(b, "uuid", uuid)
            compareBytes(b, "address", host)

            executePreparedStatement(connection, preparedInsert, listOf(uuid, host))

            val otherResult = assertNotNull(executePreparedStatement(connection, select).rows)

            compareBytes(otherResult[1], "uuid", uuid)
            compareBytes(otherResult[1], "address", host)
        }
    }

    @Test
    fun `support BINARY type`() {

        val create =
            """CREATE TEMPORARY TABLE POSTS (
           id INT NOT NULL AUTO_INCREMENT,
           binary_column BINARY(20),
           primary key (id))
        """

        val insert = "INSERT INTO POSTS (binary_column) VALUES (?)"
        val select = "SELECT * FROM POSTS"
        val bytes = (1..10).map { it.toByte() }.toByteArray()
        // val padding = bytes[10]

        withConnection { connection ->
            executeQuery(connection, create)
            executePreparedStatement(connection, insert, listOf(bytes))
            val row = executeQuery(connection, select).rows.get(0)
            assertEquals(1, row.get("id"))
            assertNotNull(row.get("binary_column"))
            // row("binary_column") === bytes++ padding
        }
    }

    @Test
    fun `support VARBINARY type`() {

        val create = """CREATE TEMPORARY TABLE POSTS (
           id INT NOT NULL AUTO_INCREMENT,
           varbinary_column VARBINARY(20),
           primary key (id))
        """

        val insert = "INSERT INTO POSTS (varbinary_column) VALUES (?)"
        val select = "SELECT * FROM POSTS"
        val bytes = (1..10).map { i -> i.toByte() }.toByteArray()

        withConnection { connection ->
            executeQuery(connection, create)
            executePreparedStatement(connection, insert, listOf(bytes))
            val row = assertNotNull(executeQuery(connection, select).rows.get(0))
            assertEquals(1, row["id"])
            assertArrayEquals(bytes, row["varbinary_column"] as ByteArray)
        }
    }

    @Test
    fun `support BLOB type`() {
        val bytes = (1..10).map { i -> i.toByte() }.toByteArray()
        testBlob(bytes)
    }

    @Test
    fun `support BLOB type with large values`() {
        val bytes = (1..2100).map { it.toByte() }.toByteArray()
        testBlob(bytes)
    }

    fun testBlob(bytes: ByteArray) {
        val create = """CREATE TEMPORARY TABLE POSTS (
         id INT NOT NULL,
         blob_column BLOB,
         primary key (id))
      """

        val insert = "INSERT INTO POSTS (id,blob_column) VALUES (?,?)"
        val select = "SELECT id,blob_column FROM POSTS ORDER BY id"

        withConnection { connection ->
            executeQuery(connection, create)
            executePreparedStatement(connection, insert, listOf(1, bytes))
            executePreparedStatement(connection, insert, listOf(2, ByteBuffer.wrap(bytes)))
            executePreparedStatement(connection, insert, listOf(3, Unpooled.wrappedBuffer(bytes)))

            val rows = assertNotNull(executeQuery(connection, select).rows)
            assertEquals(3, rows.size)
            assertEquals(rows[0]["id"], 1)
            assertArrayEquals(rows[0]["blob_column"] as ByteArray, bytes)
            assertEquals(rows[1]["id"], 2)
            assertArrayEquals(rows[1]["blob_column"] as ByteArray, bytes)
            assertEquals(rows[2]["id"], 3)
            assertArrayEquals(rows[2]["blob_column"] as ByteArray, bytes)
        }
    }

    fun compareBytes(row: RowData, column: String, expected: String) {
        assertArrayEquals(row[column] as ByteArray, expected.toByteArray(CharsetUtil.UTF_8))
    }
}
