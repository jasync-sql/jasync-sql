package com.github.jasync.sql.db.mysql

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ExecuteManyQueriesSpec : ConnectionHelper() {

  @Test
  fun `execute many queries one after the other` () {

      withConnection {
        connection ->
        (1 .. 500).forEach {
          _ ->
          val rows  = assertNotNull(executeQuery(connection, "SELECT 6578, 'this is some text'").rows)
          assertEquals(1, rows.size)
          val row = assertNotNull(rows[0])
          assertEquals(6578L, row.get(0))
          assertEquals("this is some text", row.get(1))
          }
      }
    }

  @Test
    fun `execute many prepared statements one after the other` () {
      withConnection {
        connection ->
        (1..500).forEach {
          _ ->
              val rows  = assertNotNull(executePreparedStatement(connection, "SELECT 6578, 'this is some text'").rows)
              assertEquals(1, rows.size)
              val row = assertNotNull(rows[0])
              assertEquals(6578L, row.get(0))
              assertEquals("this is some text", row.get(1))
          }
      }
    }
  }
