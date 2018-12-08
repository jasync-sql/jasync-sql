package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.Configuration
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.time.Duration
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class QueryTimeoutSpec : ConnectionHelper() {

  @Test
  fun `"Simple query with 1 nanosec timeout"`() {
    withConfigurablePool(shortTimeoutConfiguration()) { pool ->
      {
        val connection = pool.take().get(10, TimeUnit.SECONDS)
        assertThat(connection.isTimeout()).isEqualTo(false)
        assertThat(connection.isConnected()).isEqualTo(true)
        val queryResultFuture = connection.sendQuery("select sleep(1)")
        verifyException(TimeoutException::class.java) {
          queryResultFuture.get(10, TimeUnit.SECONDS)
        }
        assertThat(connection.isTimeout()).isEqualTo(true)
        pool.giveBack(connection).get(10, TimeUnit.SECONDS)
        assertThat(pool.availables().size).isEqualTo(0) // connection removed from pool)
        // we do not know when the connection will be closed.
      }
    }
  }

  @Test
  fun `"Simple query with 5 sec timeout"`() {
    withConfigurablePool(longTimeoutConfiguration()) { pool ->
      {
        val connection = pool.take().get(10, TimeUnit.SECONDS)
        assertThat(connection.isTimeout()).isEqualTo(false)
        assertThat(connection.isConnected()).isEqualTo(true)
        val queryResultFuture = connection.sendQuery("select sleep(1)")
        assertThat((queryResultFuture.get(10, TimeUnit.SECONDS)).rows!!.size).isEqualTo(1)
        assertThat(connection.isTimeout()).isEqualTo(false)
        assertThat(connection.isConnected()).isEqualTo(true)
        pool.giveBack(connection).get(10, TimeUnit.SECONDS)
        assertThat(pool.availables().size).isEqualTo(1) // connection returned to pool)
      }
    }
  }

  fun shortTimeoutConfiguration() = Configuration(
      "mysql_async",
      "localhost",
      port = 3306,
      password = ("root"),
      database = ("mysql_async_tests"),
      queryTimeout = (Duration.ofNanos(1))
  )

  private fun longTimeoutConfiguration() = Configuration(
      "mysql_async",
      "localhost",
      port = 3306,
      password = ("root"),
      database = ("mysql_async_tests"),
      queryTimeout = (Duration.ofSeconds(5))
  )

}
