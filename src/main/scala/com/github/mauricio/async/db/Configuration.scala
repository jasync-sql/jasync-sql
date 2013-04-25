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
import util.ExecutorServiceUtils

object Configuration {
  val Default = new Configuration("postgres")
}

/**
 *
 * Contains the configuration to be able to connect to a database.
 *
 * @param username
 * @param host
 * @param port
 * @param password
 * @param database
 * @param bossPool
 * @param workerPool
 * @param charset
 */

case class Configuration(val username: String,
                         val host: String = "localhost",
                         val port: Int = 5432,
                         val password: Option[String] = None,
                         val database: Option[String] = None,
                         val bossPool: ExecutorService = ExecutorServiceUtils.CachedThreadPool,
                         val workerPool: ExecutorService = ExecutorServiceUtils.CachedThreadPool,
                         val charset: Charset = CharsetUtil.UTF_8
                          )
