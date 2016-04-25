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

import java.nio.charset.Charset

import com.github.mauricio.async.db.{Configuration, SSLConfiguration}
import com.github.mauricio.async.db.exceptions.UnableToParseURLException
import io.netty.buffer.{ByteBufAllocator, PooledByteBufAllocator}
import org.specs2.mutable.Specification

import scala.concurrent.duration.Duration

class URLParserSpec extends Specification {

  "mysql URLParser" should {
    import URLParser.{DEFAULT, parse, parseOrDie}


    "have a reasonable default" in {
      // This is a deliberate extra step, protecting the DEFAULT from frivilous changes.
      // Any change to DEFAULT should require a change to this test.

      DEFAULT === Configuration(
        username = "root",
        host = "127.0.0.1", //Matched JDBC default
        port = 3306,
        password = None,
        database = None
      )
    }


    // Divided into sections
    // =========== jdbc:mysql ===========

    "create a jdbc:mysql connection with the available fields" in {
      val connectionUri = "jdbc:mysql://128.167.54.90:9987/my_database?user=john&password=doe"

      parse(connectionUri) === DEFAULT.copy(
        username = "john",
        password = Some("doe"),
        database = Some("my_database"),
        host = "128.167.54.90",
        port = 9987
      )
    }

    "create a connection without port" in {
      val connectionUri = "jdbc:mysql://128.167.54.90/my_database?user=john&password=doe"

      parse(connectionUri) === DEFAULT.copy(
        username = "john",
        password = Some("doe"),
        database = Some("my_database"),
        host = "128.167.54.90"
      )
    }


    "create a connection without username and password" in {
      val connectionUri = "jdbc:mysql://128.167.54.90:9987/my_database"

      parse(connectionUri) === DEFAULT.copy(
        database = Some("my_database"),
        host = "128.167.54.90",
        port = 9987
      )
    }

    "create a connection from a heroku like URL using 'mysql' protocol" in {
      val connectionUri = "mysql://john:doe@128.167.54.90:9987/my_database"

      parse(connectionUri) === DEFAULT.copy(
        username = "john",
        password = Some("doe"),
        database = Some("my_database"),
        host = "128.167.54.90",
        port = 9987
      )
    }

    "create a connection with the available fields and named server" in {
      val connectionUri = "jdbc:mysql://localhost:9987/my_database?user=john&password=doe"

      parse(connectionUri) === DEFAULT.copy(
        username = "john",
        password = Some("doe"),
        database = Some("my_database"),
        host = "localhost",
        port = 9987
      )
    }

    "create a connection from a heroku like URL with named server" in {
      val connectionUri = "mysql://john:doe@psql.heroku.com:9987/my_database"

      val configuration = parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "psql.heroku.com"
      configuration.port === 9987
    }

    "create a connection with the available fields and ipv6" in {
      val connectionUri = "jdbc:mysql://[::1]:9987/my_database?user=john&password=doe"

      val configuration = parse(connectionUri)

      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "::1"
      configuration.port === 9987
    }

    "create a connection from a heroku like URL and with ipv6" in {
      val connectionUri = "mysql://john:doe@[::1]:9987/my_database"

      val configuration = parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "::1"
      configuration.port === 9987
    }

    "create a connection with a missing hostname" in {
      val connectionUri = "jdbc:mysql:/my_database?user=john&password=doe"

      parse(connectionUri) === DEFAULT.copy(
        username = "john",
        password = Some("doe"),
        database = Some("my_database")
      )
    }

    "create a connection with a missing database name" in {
      val connectionUri = "jdbc:mysql://[::1]:9987/?user=john&password=doe"

      val configuration = parse(connectionUri)

      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === None
      configuration.host === "::1"
      configuration.port === 9987
    }

    "create a connection with all default fields" in {
      val connectionUri = "jdbc:mysql:"

      val configuration = parse(connectionUri)

      configuration.username === "root"
      configuration.password === None
      configuration.database === None
      configuration.host === "127.0.0.1"
      configuration.port === 3306
    }

    "create a connection with an empty (invalid) url" in {
      val connectionUri = ""

      val configuration = parse(connectionUri)

      configuration.username === "root"
      configuration.password === None
      configuration.database === None
      configuration.host === "127.0.0.1"
      configuration.port === 3306
    }


    "recognise a mysql:// uri" in {
      parse("mysql://localhost:425/dbname") mustEqual DEFAULT.copy(
        username = "root",
        database = Some("dbname"),
        port = 425,
        host = "localhost"
      )
    }

    "recognize a jdbc:mysql:// uri" in {
      parse("jdbc:mysql://localhost:425/dbname") mustEqual DEFAULT.copy(
        username = "root",
        database = Some("dbname"),
        port = 425,
        host = "localhost"
      )
    }

    "pull the username and password from URI credentials" in {
      parse("jdbc:mysql://user:password@localhost:425/dbname") mustEqual DEFAULT.copy(
        username = "user",
        password = Some("password"),
        database = Some("dbname"),
        port = 425,
        host = "localhost"
      )
    }

    "pull the username and password from query string" in {
      parse("jdbc:mysql://localhost:425/dbname?user=user&password=password") mustEqual DEFAULT.copy(
        username = "user",
        password = Some("password"),
        database = Some("dbname"),
        port = 425,
        host = "localhost"
      )
    }

    // Included for consistency, so later changes aren't allowed to change behavior
    "use the query string parameters to override URI credentials" in {
      parse("jdbc:mysql://baduser:badpass@localhost:425/dbname?user=user&password=password") mustEqual DEFAULT.copy(
        username = "user",
        password = Some("password"),
        database = Some("dbname"),
        port = 425,
        host = "localhost"
      )
    }

    "successfully default the port to the mysql port" in {
      parse("jdbc:mysql://baduser:badpass@localhost/dbname?user=user&password=password") mustEqual DEFAULT.copy(
        username = "user",
        password = Some("password"),
        database = Some("dbname"),
        port = 3306,
        host = "localhost"
      )
    }

    "reject malformed ip addresses" in {
      val connectionUri = "mysql://john:doe@128.567.54.90:9987/my_database"

      val configuration = parse(connectionUri)
      configuration.username === "root"
      configuration.password === None
      configuration.database === None
      configuration.host === "127.0.0.1"
      configuration.port === 3306

      parseOrDie(connectionUri) must throwA[UnableToParseURLException]
    }

  }

}
