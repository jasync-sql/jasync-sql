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

import com.github.mauricio.async.db.util.ExecutorServiceUtils
import java.nio.charset.Charset
import java.util.concurrent.ExecutorService
import scala.{None, Option, Int}
import scala.Predef._
import org.jboss.netty.util.CharsetUtil

object Configuration {
  val DefaultCharset = CharsetUtil.UTF_8
  val Default = new Configuration("postgres")
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

case class Configuration(username: String,
                         host: String = "localhost",
                         port: Int = 5432,
                         password: Option[String] = None,
                         database: Option[String] = None,
                         bossPool: ExecutorService = ExecutorServiceUtils.CachedThreadPool,
                         workerPool: ExecutorService = ExecutorServiceUtils.CachedThreadPool,
                         charset: Charset = Configuration.DefaultCharset,
                         maximumMessageSize: Int = 16777216
                          )
