package com.github.jasync.sql.db.mysql

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class BitSpec : ConnectionHelper() {


    @Test
    fun `result in binary data` () {

      withConnection {
        connection ->
          val create = """CREATE TEMPORARY TABLE binary_test
                         (
                           id INT NOT NULL AUTO_INCREMENT,
                           some_bit BIT(1) NOT NULL,
                           PRIMARY KEY (id)
                         )"""

          executeQuery(connection, create)
          executePreparedStatement(connection,
            "INSERT INTO binary_test (some_bit) VALUES (B'0'),(B'1')")

          val rows = assertNotNull(executePreparedStatement(connection, "select * from binary_test").rows)

          val bit0 = rows[0]["some_bit"]
          val bit1 = rows[1]["some_bit"]

          assertEquals(0, bit0)
          assertEquals(1, bit1)
      }

    }

    @Test
    fun `result in binary data in BIT(2) column` (){

      withConnection {
        connection ->
          val create = """CREATE TEMPORARY TABLE binary_test
                         (
                           id INT NOT NULL AUTO_INCREMENT,
                           some_bit BIT(2) NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id)
                         )"""

          executeQuery(connection, create)
          executePreparedStatement(connection,
            "INSERT INTO binary_test (some_bit) VALUES (B'00'),(B'01'),(B'10'),(B'11')")

          val rows = assertNotNull(executePreparedStatement(connection, "select * from binary_test").rows)

          val bit0 = rows[0]["some_bit"]
          val bit1 = rows[1]["some_bit"]
          val bit2 = rows[2]["some_bit"]
          val bit3 = rows[3]["some_bit"]

          assertEquals(0, bit0)
          assertEquals(1, bit1)
          assertEquals(2, bit2)
          assertEquals(3, bit3)
      }

    }

  }
