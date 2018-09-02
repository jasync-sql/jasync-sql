package com.github.mauricio.async.db.postgresql.pool

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.pool.ObjectFactory
import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import com.github.jasync.sql.db.util.Try
import com.github.mauricio.async.db.postgresql.PostgreSQLConnection
import io.netty.channel.EventLoopGroup
import mu.KotlinLogging
import java.nio.channels.ClosedChannelException
import java.util.concurrent.ExecutorService

private val logger = KotlinLogging.logger {}

/**
 *
 * Object responsible for creating connection instances.
 *
 * @param configuration
 */

class PostgreSQLConnectionFactory(val configuration: Configuration,
                                  group: EventLoopGroup = NettyUtils.DefaultEventLoopGroup,
                                  executionContext: ExecutorService = ExecutorServiceUtils.CachedThreadPool) : ObjectFactory<PostgreSQLConnection> {

  fun create: PostgreSQLConnection() {
    val connection = PostgreSQLConnection(configuration, group = group, executionContext = executionContext)
    Await.result(connection.connect, configuration.connectTimeout)

    connection
  }

  fun destroy(item: PostgreSQLConnection) {
    item.disconnect
  }

  /**
   *
   * Validates by checking if the connection is still connected to the database or not.
   *
   * @param item an object produced by this pool
   * @return
   */

  fun validate(item: PostgreSQLConnection): Try<PostgreSQLConnection> {
    Try {
      if (item.isTimeouted) {
        throw ConnectionTimeoutedException(item)
      }
      if (!item.isConnected || item.hasRecentError) {
        throw ClosedChannelException()
      }
      item.validateIfItIsReadyForQuery("Trying to give back a connection that is not ready for query")
      item
    }
  }

  /**
   *
   * Tests whether we can still send a **SELECT 0** statement to the database.
   *
   * @param item an object produced by this pool
   * @return
   */

  override fun test(item: PostgreSQLConnection): Try<PostgreSQLConnection> {
    val result: Try<PostgreSQLConnection> = Try({
      Await.result(item.sendQuery("SELECT 0"), configuration.testTimeout)
      item
    })

    result when {
      Failure(e) -> {
        try {
          if (item.isConnected) {
            item.disconnect
          }
        } catch {
          e : Exception -> log.error("Failed disconnecting object", e)
        }
        result
      }
      Success(i) -> {
        result
      }
    }
  }

}