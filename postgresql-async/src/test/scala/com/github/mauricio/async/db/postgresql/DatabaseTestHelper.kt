
package com.github.mauricio.async.db.postgresql

import com.github.mauricio.async.db.util.Log
import com.github.mauricio.async.db.Connection
import com.github.mauricio.async.db.Configuration
import java.io.File
import java.util.concurrent.TimeoutException
import java.util.concurrent.TimeUnit
import scala.concurrent.duration.*
import scala.concurrent.Future
import scala.concurrent.Await
import com.github.mauricio.async.db.SSLConfiguration
import com.github.mauricio.async.db.SSLConfiguration.Mode

object DatabaseTestHelper {
  val log = Log.get<DatabaseTestHelper>
}

interface DatabaseTestHelper {


  fun databaseName ()= Some("netty_driver_test")

  fun timeTestDatabase ()= Some("netty_driver_time_test")

  fun databasePort ()= 5432

  fun defaultConfiguration ()= Configuration(
    port = databasePort,
    username = "postgres",
    database = databaseName)

  fun timeTestConfiguration ()= Configuration(
    port = databasePort,
    username = "postgres",
    database = timeTestDatabase)

  fun ,Handler<T>(fn: (PostgreSQLConnection) -> T): T {
    ,Handler(this.defaultConfiguration, fn)
  }

  fun ,TimeHandler<T>(fn: (PostgreSQLConnection) -> T): T {
    ,Handler(this.timeTestConfiguration, fn)
  }

  fun ,SSLHandler<T>(mode: SSLConfiguration.Mode.Value, host: String = "localhost", rootCert: Option<File> = Some(new File("script/server.crt")))(fn: (PostgreSQLConnection) -> T): T {
    val config = Configuration(
    host = host,
    port = databasePort,
    username = "postgres",
    database = databaseName,
    ssl = SSLConfiguration(mode = mode, rootCert = rootCert))
    ,Handler(config, fn)
  }

  fun ,Handler<T>(configuration: Configuration, fn: (PostgreSQLConnection) -> T): T {

    val handler = PostgreSQLConnection(configuration)

    try {
      Await.result(handler.connect, Duration(5, SECONDS))
      fn(handler)
    } finally {
      handleTimeout(handler, handler.disconnect)
    }

  }

  fun executeDdl(handler: Connection, data: String, count: Int = 0) {
    val rows = handleTimeout(handler, {
      Await.result(handler.sendQuery(data), Duration(5, SECONDS)).rowsAffected
    })

    if (rows != count) {
      throw IllegalStateException("We expected %s rows but there were %s".format(count, rows))
    }

    rows
  }

  private fun handleTimeout<R>( handler : Connection, fn : -> R ) {
    try {
      fn
    } catch {
      e : TimeoutException -> {
        throw IllegalStateException("Timeout executing call from handler -> %s".format( handler))
      }
    }
  }

  fun executeQuery(handler: Connection, data: String) {
    handleTimeout( handler, {
      Await.result(handler.sendQuery(data), Duration(5, SECONDS))
    } )
  }

  fun executePreparedStatement(
                                handler: Connection,
                                statement: String,
                                values: Array<Any> = Array.empty<Any>) {
    handleTimeout( handler, {
      Await.result(handler.sendPreparedStatement(statement, values), Duration(5, SECONDS))
    } )
  }

  fun await<T>(future: CompletableFuture<T>): T {
    Await.result(future, Duration(5, TimeUnit.SECONDS))
  }


}