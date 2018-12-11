package com.github.jasync.sql.db.mysql

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BitSpec : ConnectionHelper() {


    @Test
    fun `result in binary data`() {

        withConnection { connection ->
            val create = """CREATE TEMPORARY TABLE binary_test
                         (
                           id INT NOT NULL AUTO_INCREMENT,
                           some_bit BIT(1) NOT NULL,
                           PRIMARY KEY (id)
                         )"""

            executeQuery(connection, create)
            executePreparedStatement(
                connection,
                "INSERT INTO binary_test (some_bit) VALUES (B'0'),(B'1')"
            )

            val rows = assertNotNull(executePreparedStatement(connection, "select * from binary_test").rows)

            val bit0: ByteArray = rows[0]["some_bit"] as ByteArray
            val bit1: ByteArray = rows[1]["some_bit"] as ByteArray

            val ba = byteArrayOf(0, 1)
            assertEquals(ba[0], bit0[0])
            assertEquals(ba[1], bit1[0])
        }

    }

    @Test
    fun `result in binary data in BIT(2) column`() {

        withConnection { connection ->
            val create = """CREATE TEMPORARY TABLE binary_test
                         (
                           id INT NOT NULL AUTO_INCREMENT,
                           some_bit BIT(2) NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id)
                         )"""

            executeQuery(connection, create)
            executePreparedStatement(
                connection,
                "INSERT INTO binary_test (some_bit) VALUES (B'00'),(B'01'),(B'10'),(B'11')"
            )

            val rows = assertNotNull(executePreparedStatement(connection, "select * from binary_test").rows)

            val bit0 = rows[0]["some_bit"] as ByteArray
            val bit1 = rows[1]["some_bit"] as ByteArray
            val bit2 = rows[2]["some_bit"] as ByteArray
            val bit3 = rows[3]["some_bit"] as ByteArray

            val ba = byteArrayOf(0, 1, 2, 3)
            assertEquals(ba[0], bit0[0])
            assertEquals(ba[1], bit1[0])
            assertEquals(ba[2], bit2[0])
            assertEquals(ba[3], bit3[0])
        }

    }

}
