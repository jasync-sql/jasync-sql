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

package com.github.mauricio.async.db

import java.nio.charset.Charset
import java.util.concurrent.ExecutorService
import org.jboss.netty.util.CharsetUtil
import com.github.mauricio.async.db.util.{URLParser, ExecutorServiceUtils}
import com.github.mauricio.async.db.postgresql.MessageDecoder

object Configuration {
  val Default = new Configuration("postgres")

  /**
   *
   * Creates a [[com.github.mauricio.async.db.Configuration]] object from a database URL, accepts both the
   * JDBC URL as a Heroku like URL.
   *
   * @param url
   * @param bossPool
   * @param workerPool
   * @param charset
   * @return
   */

  def from(url: String,
            bossPool: ExecutorService = Default.bossPool,
            workerPool: ExecutorService = Default.workerPool,
            charset: Charset = Default.charset): Configuration = URLParser.parse(url, bossPool, workerPool, charset)

}

/**
 *
 * Contains the configuration necessary to connect to a database.
 *
 * @param username database username
 * @param host database host, defaults to "localhost"
 * @param port database port, defaults to 5432
 * @param password password, defaults to no password
 * @param database database name, defaults to no database
 * @param bossPool executor service used by by the Netty boss handler, defaults to the driver's
 *                 cached thread pool at [[com.github.mauricio.async.db.util.ExecutorServiceUtils]]
 * @param workerPool executor service used by the Netty worker handler, defaults to the driver's
 *                   cached thread pool at [[com.github.mauricio.async.db.util.ExecutorServiceUtils]]
 * @param charset charset for the connection, defaults to UTF-8, make sure you know what you are doing if you
 *                change this
 * @param maximumMessageSize the maximum size a message from the server could possibly have, this limits possible
 *                           OOM or eternal loop attacks the client could have, defaults to 16 MB. You can set this
 *                           to any value you would like but again, make sure you know what you are doing if you do
 *                           change it.
 */

case class Configuration(val username: String,
                         val host: String = "localhost",
                         val port: Int = 5432,
                         val password: Option[String] = None,
                         val database: Option[String] = None,
                         val bossPool: ExecutorService = ExecutorServiceUtils.FixedThreadPool,
                         val workerPool: ExecutorService = ExecutorServiceUtils.FixedThreadPool,
                         val charset: Charset = CharsetUtil.UTF_8,
                         val maximumMessageSize: Int = MessageDecoder.DefaultMaximumSize
                          )
