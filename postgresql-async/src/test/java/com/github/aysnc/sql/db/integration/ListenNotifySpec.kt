package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.postgresql.messages.backend.NotificationResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.UUID

class ListenNotifySpec : DatabaseTestHelper() {

    private fun generateQueueName() = "scala_pg_async_test_" + UUID.randomUUID().toString().replace("-", "")

    @Test
    fun `connection should be able to receive a notification if listening`() {
        withHandler { connection ->

            val queue = generateQueueName()

            executeQuery(connection, "LISTEN $queue")

            var payload = ""
            var channel = ""

            connection.registerNotifyListener { message ->
                payload = message.payload
                channel = message.channel
            }

            executeQuery(connection, "NOTIFY $queue, 'this-is-some-data'")

            Thread.sleep(1000)

            assertThat(payload).isEqualTo("this-is-some-data")
            assertThat(channel).isEqualTo(queue)

            assertThat(connection.hasRecentError()).isFalse()
        }
    }

    @Test
    fun `connection should be able to receive a notification from a pg_notify call`() {
        withHandler { connection ->
            val queue = generateQueueName()

            executeQuery(connection, "LISTEN $queue")

            var payload = ""
            var channel = ""

            connection.registerNotifyListener { message ->
                payload = message.payload
                channel = message.channel
            }

            executePreparedStatement(connection, "SELECT pg_notify(?, ?)", listOf(queue, "notifying-again"))

            Thread.sleep(1000)

            assertThat(payload).isEqualTo("notifying-again")
            assertThat(channel).isEqualTo(queue)
        }
    }

    @Test
    fun `connection should not receive any notification if not registered to the correct channel`() {
        withHandler { connection ->

            val queue = generateQueueName()
            val otherQueue = generateQueueName()

            executeQuery(connection, "LISTEN $queue")

            var payload = ""
            var channel = ""

            connection.registerNotifyListener { message ->
                payload = message.payload
                channel = message.channel
            }

            executePreparedStatement(connection, "SELECT pg_notify(?, ?)", listOf(otherQueue, "notifying-again"))

            Thread.sleep(1000)

            assertThat(payload).isEqualTo("")
            assertThat(channel).isEqualTo("")
        }
    }

    @Test
    fun `connection should not receive notifications if cleared the collection`() {
        withHandler { connection ->
            val queue = generateQueueName()

            executeQuery(connection, "LISTEN $queue")

            var payload = ""
            var channel = ""

            connection.registerNotifyListener { message ->
                payload = message.payload
                channel = message.channel
            }

            connection.clearNotifyListeners()

            executeQuery(connection, "NOTIFY $queue, 'this-is-some-data'")

            Thread.sleep(1000)

            assertThat(payload).isEqualTo("")
            assertThat(channel).isEqualTo("")
        }
    }

    @Test
    fun `connection should not receive notification if listener was removed`() {
        withHandler { connection ->
            val queue = generateQueueName()

            executeQuery(connection, "LISTEN $queue")

            var payload = ""
            var channel = ""

            val listener: (NotificationResponse) -> Unit = { message: NotificationResponse ->
                payload = message.payload
                channel = message.channel
            }

            connection.registerNotifyListener(listener)
            connection.unregisterNotifyListener(listener)

            executeQuery(connection, "NOTIFY $queue, 'this-is-some-data'")

            Thread.sleep(1000)

            assertThat(payload).isEqualTo("")
            assertThat(channel).isEqualTo("")
        }
    }

    @Test
    fun `connection should be able to receive notify ,out payload`() {
        withHandler { connection ->
            val queue = generateQueueName()

            executeQuery(connection, "LISTEN $queue")

            var payload = "this is some fake payload"
            var channel = ""

            val listener: (NotificationResponse) -> Unit = { message ->
                payload = message.payload
                channel = message.channel
            }

            connection.registerNotifyListener(listener)

            executeQuery(connection, "NOTIFY $queue")

            Thread.sleep(1000)

            assertThat(payload).isEqualTo("")
            assertThat(channel).isEqualTo(queue)
        }
    }
}
