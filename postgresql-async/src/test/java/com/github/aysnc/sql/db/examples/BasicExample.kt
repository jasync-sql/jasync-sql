package com.github.aysnc.sql.db.examples

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.RowData
import com.github.jasync.sql.db.postgresql.PostgreSQLConnection
import com.github.jasync.sql.db.postgresql.util.URLParser
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.head
import com.github.jasync.sql.db.util.map
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {

  val configuration = URLParser.parse("jdbc:postgresql://localhost:5432/my_database?username=postgres&password=mysecretpassword")
  val connection: Connection = PostgreSQLConnection(configuration)

  connection.connect().get(5, TimeUnit.SECONDS)

  val future: CompletableFuture<QueryResult> = connection.sendQuery("SELECT 0")

  val mapResult: CompletableFuture<Any?> = future.map(executor = ExecutorServiceUtils.CommonPool) { queryResult ->
    val resultSet = queryResult.rows
    when {
      resultSet != null -> {
        val row: RowData = resultSet.head
        row[0]
      }
      else -> -1
    }
  }


  val result = mapResult.get(5, TimeUnit.SECONDS)

  println(result)

  connection.disconnect()

}


