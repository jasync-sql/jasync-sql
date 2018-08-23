/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
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

package com.github.mauricio.async.db.mysql

import com.github.mauricio.async.db.util.Log
import java.util.concurrent.atomic.AtomicInteger

/**
 * Mainly a way to try to figure out why sometimes MySQL will fail with a bad prepared statement response message.
 */

object ConcurrentlyRunTest extends ConnectionHelper with Runnable {

  private val log = Log.getByName(this.getClass.getName)
  private val counter = new AtomicInteger()
  private val failures = new AtomicInteger()

  def run() {
    1.until(50).foreach(x => execute(counter.incrementAndGet()))
  }

  def main(args : Array[String]) {

    log.info("Starting executing code")

    val threads = 1.until(10).map(x => new Thread(this))

    threads.foreach {t => t.start()}

    while ( !threads.forall(t => t.isAlive) ) {
      Thread.sleep(5000)
    }

    log.info(s"Finished executing code, failed execution ${failures.get()} times")

  }


  def execute(count : Int) {
    try {
      log.info(s"====> run $count")
      val create = """CREATE TEMPORARY TABLE posts (
                     |       id INT NOT NULL AUTO_INCREMENT,
                     |       some_text TEXT not null,
                     |       some_date DATE,
                     |       primary key (id) )""".stripMargin

      val insert = "insert into posts (some_text) values (?)"
      val select = "select * from posts limit 100"

      withConnection {
        connection =>
          executeQuery(connection, create)

          executePreparedStatement(connection, insert, "this is some text here")

          val row = executeQuery(connection, select).rows.get(0)
          assert(row("id") == 1)
          assert(row("some_text") == "this is some text here")
          assert(row("some_date") == null)

          val queryRow = executePreparedStatement(connection, select).rows.get(0)

          assert(queryRow("id") == 1)
          assert(queryRow("some_text") == "this is some text here")
          assert(queryRow("some_date") == null)

      }
    } catch {
      case e : Exception => {
        failures.incrementAndGet()
        log.error( s"Failed to execute on run $count - ${e.getMessage}", e)
      }
    }

  }

}
