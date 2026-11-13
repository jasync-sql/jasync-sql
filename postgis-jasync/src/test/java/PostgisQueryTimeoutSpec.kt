package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.exceptions.ConnectionStillRunningQueryException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.Test
import java.time.Duration
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class PostgisQueryTimeoutSpec : DatabaseTestHelper() {

    @Test
    fun `query timeout inside transaction should clear running query before rollback on same connection`() {
        val timeoutConfiguration = conf.copy(queryTimeout = Duration.ofSeconds(1))

        withHandler(timeoutConfiguration) { connection ->
            executeQuery(
                connection,
                """
                CREATE OR REPLACE FUNCTION slow_jasync_function() RETURNS void AS $$ BEGIN PERFORM pg_sleep(5); END; $$ LANGUAGE plpgsql
                """.trimIndent()
            )
            executeQuery(connection, "BEGIN")

            // The query times out after one second; TimeoutScheduler fails the pending promise first and then
            // triggers onQueryTimeout()/disconnect(), so this must surface as ExecutionException(TimeoutException).
            val future = connection.sendQuery("SELECT slow_jasync_function()")
            assertThatThrownBy {
                future.get(10, TimeUnit.SECONDS)
            }
                .isInstanceOf(ExecutionException::class.java)
                .hasCauseInstanceOf(TimeoutException::class.java)

            // Wait for the timeout-driven disconnect to actually close the physical channel.
            await.atMost(10, TimeUnit.SECONDS).untilCallTo { connection.isConnected() } matches { it == false }

            assertThat(connection.isQuerying()).isFalse()

            // Simulate the pool reusing the same physical connection for cleanup. The connection is already
            // closed, so IllegalStateException is acceptable; what must never happen is that the stale query
            // promise turns the cleanup failure into ConnectionStillRunningQueryException.
            assertThatThrownBy {
                connection.sendQuery("ROLLBACK")
            }.isNotInstanceOf(ConnectionStillRunningQueryException::class.java)
        }
    }
}
