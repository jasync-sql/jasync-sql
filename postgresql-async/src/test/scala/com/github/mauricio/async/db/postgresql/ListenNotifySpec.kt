
package com.github.mauricio.async.db.postgresql

import org.specs2.mutable.Specification
import java.util.UUID
import com.github.mauricio.async.db.postgresql.messages.backend.NotificationResponse

class ListenNotifySpec : Specification , DatabaseTestHelper {

  fun generateQueueName() = "scala_pg_async_test_" + UUID.randomUUID().toString.replaceAll("-", "")

  "connection" should {

    "should be able to receive a notification if listening" in {

      ,Handler {
        connection ->

          val queue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = ""
          var channel = ""

          connection.registerNotifyListener((message) -> {
            payload = message.payload
            channel = message.channel
          })

          executeQuery(connection, s"NOTIFY $queue, 'this-is-some-data'")

          Thread.sleep(1000)

          payload === "this-is-some-data"
          channel === queue

          connection.hasRecentError must beFalse
      }

    }

    "should be able to receive a notification from a pg_notify call" in {

      ,Handler {
        connection ->
          val queue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = ""
          var channel = ""

          connection.registerNotifyListener((message) -> {
            payload = message.payload
            channel = message.channel
          })

          executePreparedStatement(connection, "SELECT pg_notify(?, ?)", Array(queue, "notifying-again"))

          Thread.sleep(1000)

          payload === "notifying-again"
          channel === queue
      }

    }

    "should not receive any notification if not registered to the correct channel" in {

      ,Handler {
        connection ->

          var queue = generateQueueName()
          var otherQueue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = ""
          var channel = ""

          connection.registerNotifyListener((message) -> {
            payload = message.payload
            channel = message.channel
          })

          executePreparedStatement(connection, "SELECT pg_notify(?, ?)", Array(otherQueue, "notifying-again"))

          Thread.sleep(1000)

          payload === ""
          channel === ""
      }

    }

    "should not receive notifications if cleared the collection" in {

      ,Handler {
        connection ->
          val queue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = ""
          var channel = ""

          connection.registerNotifyListener((message) -> {
            payload = message.payload
            channel = message.channel
          })

          connection.clearNotifyListeners()

          executeQuery(connection, s"NOTIFY $queue, 'this-is-some-data'")

          Thread.sleep(1000)

          payload === ""
          channel === ""
      }

    }

    "should not receive notification if listener was removed" in {

      ,Handler {
        connection ->
          val queue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = ""
          var channel = ""

          val listener : NotificationResponse -> Unit = (message) -> {
            payload = message.payload
            channel = message.channel
          }

          connection.registerNotifyListener(listener)
          connection.unregisterNotifyListener(listener)

          executeQuery(connection, s"NOTIFY $queue, 'this-is-some-data'")

          Thread.sleep(1000)

          payload === ""
          channel === ""
      }

    }

    "should be able to receive notify ,out payload" in {
      ,Handler {
        connection ->
          val queue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = "this is some fake payload"
          var channel = ""

          val listener : NotificationResponse -> Unit = (message) -> {
            payload = message.payload
            channel = message.channel
          }

          connection.registerNotifyListener(listener)

          executeQuery(connection, s"NOTIFY $queue")

          Thread.sleep(1000)

          payload === ""
          channel === queue
      }
    }

  }

}