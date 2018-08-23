/*
 * Copyright 2016 Maurício Linhares
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
package com.github.mauricio.async.db.mysql.util

import com.github.mauricio.async.db.util.AbstractURIParser
import com.github.mauricio.async.db.Configuration

/**
 * The MySQL URL parser.
 */
object URLParser extends AbstractURIParser {

  /**
   * The default configuration for MySQL.
   */
  override val DEFAULT = Configuration(
    username = "root",
    host = "127.0.0.1", //Matched JDBC default
    port = 3306,
    password = None,
    database = None
  )

  override protected val SCHEME = "^mysql$".r

}
