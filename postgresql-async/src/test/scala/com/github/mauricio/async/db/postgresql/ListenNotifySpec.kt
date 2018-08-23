/*
 * Copyright 2013-2014 db-sql-common
 *
 * The db-sql-common project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.postgresql

import org.specs2.mutable.Specification
import java.util.UUID
import com.github.mauricio.async.db.postgresql.messages.backend.NotificationResponse

class ListenNotifySpec extends Specification with DatabaseTestHelper {

  def generateQueueName() = "scala_pg_async_test_" + UUID.randomUUID().toString.replaceAll("-", "")

  "connection" should {

    "should be able to receive a notification if listening" in {

      withHandler {
        connection =>

          val queue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = ""
          var channel = ""

          connection.registerNotifyListener((message) => {
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

      withHandler {
        connection =>
          val queue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = ""
          var channel = ""

          connection.registerNotifyListener((message) => {
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

      withHandler {
        connection =>

          var queue = generateQueueName()
          var otherQueue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = ""
          var channel = ""

          connection.registerNotifyListener((message) => {
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

      withHandler {
        connection =>
          val queue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = ""
          var channel = ""

          connection.registerNotifyListener((message) => {
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

      withHandler {
        connection =>
          val queue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = ""
          var channel = ""

          val listener : NotificationResponse => Unit = (message) => {
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

    "should be able to receive notify without payload" in {
      withHandler {
        connection =>
          val queue = generateQueueName()

          executeQuery(connection, s"LISTEN $queue")

          var payload = "this is some fake payload"
          var channel = ""

          val listener : NotificationResponse => Unit = (message) => {
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
