
package com.github.mauricio.async.db.postgresql.util

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.SSLConfiguration.Mode
import com.github.mauricio.async.db.exceptions.UnableToParseURLException

class URLParserSpec : Specification {

  "postgresql URLParser" should {
    import URLParser.{parse, parseOrDie, DEFAULT}

    // Divided into sections
    // =========== jdbc:postgresql ===========

    // https://jdbc.postgresql.org/documentation/80/connect.html
    "recognize a jdbc:postgresql:dbname uri" in {
      val connectionUri = "jdbc:postgresql:dbname"

      parse(connectionUri) mustEqual DEFAULT.copy(
        database = Some("dbname")
      )
    }

    "create a jdbc:postgresql connection , the available fields" in {
      val connectionUri = "jdbc:postgresql://128.167.54.90:9987/my_database?user=john&password=doe"

      val configuration = parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "128.167.54.90"
      configuration.port === 9987
    }

    "create a connection ,out port" in {
      val connectionUri = "jdbc:postgresql://128.167.54.90/my_database?user=john&password=doe"

      val configuration = parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "128.167.54.90"
      configuration.port === 5432
    }


    "create a connection ,out username and password" in {
      val connectionUri = "jdbc:postgresql://128.167.54.90:9987/my_database"

      val configuration = parse(connectionUri)
      configuration.username === DEFAULT.username
      configuration.password === None
      configuration.database === Some("my_database")
      configuration.host === "128.167.54.90"
      configuration.port === 9987
    }

    //========== postgresql:// ==============

    "create a connection from a heroku like URL using 'postgresql' protocol" in {
      val connectionUri = "postgresql://john:doe@128.167.54.90:9987/my_database"

      val configuration = parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "128.167.54.90"
      configuration.port === 9987
    }

    "create a connection , SSL enabled" in {
      val connectionUri = "jdbc:postgresql://128.167.54.90:9987/my_database?sslmode=verify-full"

      val configuration = parse(connectionUri)
      configuration.username === DEFAULT.username
      configuration.password === None
      configuration.database === Some("my_database")
      configuration.host === "128.167.54.90"
      configuration.port === 9987
      configuration.ssl.mode === Mode.VerifyFull
    }

    "create a connection , SSL enabled and root CA from a heroku like URL using 'postgresql' protocol" in {
      val connectionUri = "postgresql://john:doe@128.167.54.90:9987/my_database?sslmode=verify-ca&sslrootcert=server.crt"

      val configuration = parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "128.167.54.90"
      configuration.port === 9987
      configuration.ssl.mode === Mode.VerifyCA
      configuration.ssl.rootCert.map(_.getPath) === Some("server.crt")
    }

    "create a connection , the available fields and named server" in {
      val connectionUri = "jdbc:postgresql://localhost:9987/my_database?user=john&password=doe"

      val configuration = parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "localhost"
      configuration.port === 9987
    }

    "create a connection from a heroku like URL , named server" in {
      val connectionUri = "postgresql://john:doe@psql.heroku.com:9987/my_database"

      val configuration = parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "psql.heroku.com"
      configuration.port === 9987
    }

    "create a connection , the available fields and ipv6" in {
      val connectionUri = "jdbc:postgresql://::1>:9987/my_database?user=john&password=doe"

      val configuration = parse(connectionUri)

      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "::1"
      configuration.port === 9987
    }

    "create a connection from a heroku like URL and , ipv6" in {
      val connectionUri = "postgresql://john:doe@::1>:9987/my_database"

      val configuration = parse(connectionUri)
      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "::1"
      configuration.port === 9987
    }

    "create a connection , a missing hostname" in {
      val connectionUri = "jdbc:postgresql:/my_database?user=john&password=doe"

      val configuration = parse(connectionUri)

      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === Some("my_database")
      configuration.host === "localhost"
      configuration.port === 5432
    }

    "create a connection , a missing database name" in {
      val connectionUri = "jdbc:postgresql://::1>:9987/?user=john&password=doe"

      val configuration = parse(connectionUri)

      configuration.username === "john"
      configuration.password === Some("doe")
      configuration.database === None
      configuration.host === "::1"
      configuration.port === 9987
    }

    "create a connection , all default fields" in {
      val connectionUri = "jdbc:postgresql:"

      val configuration = parse(connectionUri)

      configuration.username === "postgres"
      configuration.password === None
      configuration.database === None
      configuration.host === "localhost"
      configuration.port === 5432
    }

    "create a connection , an empty (invalid) url" in {
      val connectionUri = ""

      val configuration = parse(connectionUri)

      configuration.username === "postgres"
      configuration.password === None
      configuration.database === None
      configuration.host === "localhost"
      configuration.port === 5432
    }


    "recognise a postgresql:// uri" in {
      parse("postgresql://localhost:425/dbname") mustEqual DEFAULT.copy(
        username = "postgres",
        database = Some("dbname"),
        port = 425,
        host = "localhost"
      )
    }

    "recognise a postgres:// uri" in {
      parse("postgres://localhost:425/dbname") mustEqual DEFAULT.copy(
        username = "postgres",
        database = Some("dbname"),
        port = 425,
        host = "localhost"
      )
    }

    "recognize a jdbc:postgresql:// uri" in {
      parse("jdbc:postgresql://localhost:425/dbname") mustEqual DEFAULT.copy(
        username = "postgres",
        database = Some("dbname"),
        port = 425,
        host = "localhost"
      )
    }

    "pull the username and password from URI credentials" in {
      parse("jdbc:postgresql://user:password@localhost:425/dbname") mustEqual DEFAULT.copy(
        username = "user",
        password = Some("password"),
        database = Some("dbname"),
        port = 425,
        host = "localhost"
      )
    }

    "pull the username and password from query string" in {
      parse("jdbc:postgresql://localhost:425/dbname?user=user&password=password") mustEqual DEFAULT.copy(
        username = "user",
        password = Some("password"),
        database = Some("dbname"),
        port = 425,
        host = "localhost"
      )
    }

    // Included for consistency, so later changes aren't allowed to change behavior
    "use the query string parameters to override URI credentials" in {
      parse("jdbc:postgresql://baduser:badpass@localhost:425/dbname?user=user&password=password") mustEqual DEFAULT.copy(
        username = "user",
        password = Some("password"),
        database = Some("dbname"),
        port = 425,
        host = "localhost"
      )
    }

    "successfully default the port to the PostgreSQL port" in {
      parse("jdbc:postgresql://baduser:badpass@localhost/dbname?user=user&password=password") mustEqual DEFAULT.copy(
        username = "user",
        password = Some("password"),
        database = Some("dbname"),
        port = 5432,
        host = "localhost"
      )
    }

    "reject malformed ip addresses" in {
      val connectionUri = "postgresql://john:doe@128.567.54.90:9987/my_database"

      val configuration = parse(connectionUri)
      configuration.username === "postgres"
      configuration.password === None
      configuration.database === None
      configuration.host === "localhost"
      configuration.port === 5432

      parseOrDie(connectionUri) must throwA<UnableToParseURLException>
    }

  }

}