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

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.Configuration

class URLParserSpec extends Specification {

  "parser" should {

    "create a connection with the available fields" in {
      val connectionUri = "jdbc:postgresql://128.567.54.90:9987/my_database?username=john&password=doe"

      val configuration = URLParser.parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "128.567.54.90"
      configuration.port === 9987
    }

    "create a connection without port" in {
      val connectionUri = "jdbc:postgresql://128.567.54.90/my_database?username=john&password=doe"

      val configuration = URLParser.parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "128.567.54.90"
      configuration.port === 5432
    }


    "create a connection without username and password" in {
      val connectionUri = "jdbc:postgresql://128.567.54.90:9987/my_database"

      val configuration = URLParser.parse(connectionUri)
      configuration.username === Configuration.Default.username
      configuration.password === None
      configuration.database === Some("my_database")
      configuration.host === "128.567.54.90"
      configuration.port === 9987
    }

    "create a connection from a heroku like URL using 'postgresql' protocol" in {
      val connectionUri = "postgresql://john:doe@128.567.54.90:9987/my_database"

      val configuration = URLParser.parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "128.567.54.90"
      configuration.port === 9987
    }

    "create a connection from a heroku like URL using 'postgres' protocol" in {
      val connectionUri = "postgres://john:doe@128.567.54.90:9987/my_database"

      val configuration = URLParser.parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "128.567.54.90"
      configuration.port === 9987
    }

    "create a connection with the available fields and named server" in {
      val connectionUri = "jdbc:postgresql://localhost:9987/my_database?username=john&password=doe"

      val configuration = URLParser.parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "localhost"
      configuration.port === 9987
    }

    "create a connection from a heroku like URL with named server" in {
      val connectionUri = "postgresql://john:doe@psql.heroku.com:9987/my_database"

      val configuration = URLParser.parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "psql.heroku.com"
      configuration.port === 9987
    }

    "create a connection with the available fields and ipv6" in {
      val connectionUri = "jdbc:postgresql://[::1]:9987/my_database?username=john&password=doe"

      val configuration = URLParser.parse(connectionUri)

      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "::1"
      configuration.port === 9987
    }

    "create a connection from a heroku like URL and with ipv6" in {
      val connectionUri = "postgresql://john:doe@[::1]:9987/my_database"

      val configuration = URLParser.parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "::1"
      configuration.port === 9987
    }

  }

}
