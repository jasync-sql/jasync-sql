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

package com.github.mauricio.async.db.postgresql.util

import com.github.mauricio.async.db.{Configuration, SSLConfiguration}
import java.nio.charset.Charset

object URLParser {

  import Configuration.Default

  def parse(url: String,
            charset: Charset = Default.charset
             ): Configuration = {

    val properties = ParserURL.parse(url)

    val port = properties.get(ParserURL.PGPORT).getOrElse(ParserURL.DEFAULT_PORT).toInt

    new Configuration(
      username = properties.get(ParserURL.PGUSERNAME).getOrElse(Default.username),
      password = properties.get(ParserURL.PGPASSWORD),
      database = properties.get(ParserURL.PGDBNAME),
      host = properties.getOrElse(ParserURL.PGHOST, Default.host),
      port = port,
      ssl = SSLConfiguration(properties),
      charset = charset
    )

  }

}
