package com.github.aysnc.sql.db.postgresql.util

import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.exceptions.UnableToParseURLException
import com.github.jasync.sql.db.postgresql.util.URLParser
import com.github.jasync.sql.db.util.nullableMap
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test


class URLParserSpec {


    // Divided into sections
    // =========== jdbc:postgresql ===========

    // https://jdbc.postgresql.org/documentation/80/connect.html
    @Test
    fun `"postgresql URLParser" should     "recognize a jdbc_postgresql_dbname uri"`() {
        val connectionUri = "jdbc:postgresql:dbname"

        assertThat(URLParser.parse(connectionUri)).isEqualTo(
            URLParser.DEFAULT.copy(
                database = "dbname"
            )
        )
    }

    @Test
    fun `"postgresql URLParser" should     "create a jdbc_postgresql connection , the available fields"`() {
        val connectionUri = "jdbc:postgresql://128.167.54.90:9987/my_database?user=john&password=doe"

        val configuration = URLParser.parse(connectionUri)
        assertThat(configuration.username).isEqualTo("john")
        assertThat(configuration.password).isEqualTo("doe")
        assertThat(configuration.database).isEqualTo("my_database")
        assertThat(configuration.host).isEqualTo("128.167.54.90")
        assertThat(configuration.port).isEqualTo(9987)
    }

    @Test
    fun `"postgresql URLParser" should     "create a connection ,out port"`() {
        val connectionUri = "jdbc:postgresql://128.167.54.90/my_database?user=john&password=doe"

        val configuration = URLParser.parse(connectionUri)
        assertThat(configuration.username).isEqualTo("john")
        assertThat(configuration.password).isEqualTo("doe")
        assertThat(configuration.database).isEqualTo("my_database")
        assertThat(configuration.host).isEqualTo("128.167.54.90")
        assertThat(configuration.port).isEqualTo(5432)
    }


    @Test
    fun `"postgresql URLParser" should     "create a connection ,out username and password"`() {
        val connectionUri = "jdbc:postgresql://128.167.54.90:9987/my_database"

        val configuration = URLParser.parse(connectionUri)
        assertThat(configuration.username).isEqualTo(URLParser.DEFAULT.username)
        assertThat(configuration.password).isEqualTo(null)
        assertThat(configuration.database).isEqualTo("my_database")
        assertThat(configuration.host).isEqualTo("128.167.54.90")
        assertThat(configuration.port).isEqualTo(9987)
    }

    //========== postgresql:// ==============

    @Test
    fun `"postgresql URLParser" should     "create a connection from a heroku like URL using 'postgresql' protocol"`() {
        val connectionUri = "postgresql://john:doe@128.167.54.90:9987/my_database"

        val configuration = URLParser.parse(connectionUri)
        assertThat(configuration.username).isEqualTo("john")
        assertThat(configuration.password).isEqualTo("doe")
        assertThat(configuration.database).isEqualTo("my_database")
        assertThat(configuration.host).isEqualTo("128.167.54.90")
        assertThat(configuration.port).isEqualTo(9987)
    }

    @Test
    fun `"postgresql URLParser" should     "create a connection , SSL enabled"`() {
        val connectionUri = "jdbc:postgresql://128.167.54.90:9987/my_database?sslmode=verify-full"

        val configuration = URLParser.parse(connectionUri)
        assertThat(configuration.username).isEqualTo(URLParser.DEFAULT.username)
        assertThat(configuration.password).isEqualTo(null)
        assertThat(configuration.database).isEqualTo("my_database")
        assertThat(configuration.host).isEqualTo("128.167.54.90")
        assertThat(configuration.port).isEqualTo(9987)
        assertThat(configuration.ssl.mode).isEqualTo(SSLConfiguration.Mode.VerifyFull)
    }

    @Test
    fun `"postgresql URLParser" should     "create a connection , SSL enabled and root CA from a heroku like URL using 'postgresql' protocol"`() {
        val connectionUri =
            "postgresql://john:doe@128.167.54.90:9987/my_database?sslmode=verify-ca&sslrootcert=server.crt"

        val configuration = URLParser.parse(connectionUri)
        assertThat(configuration.username).isEqualTo("john")
        assertThat(configuration.password).isEqualTo("doe")
        assertThat(configuration.database).isEqualTo("my_database")
        assertThat(configuration.host).isEqualTo("128.167.54.90")
        assertThat(configuration.port).isEqualTo(9987)
        assertThat(configuration.ssl.mode).isEqualTo(SSLConfiguration.Mode.VerifyCA)
        assertThat(configuration.ssl.rootCert.nullableMap { it.path }).isEqualTo("server.crt")
    }

    @Test
    fun `"postgresql URLParser" should     "create a connection , the available fields and named server"`() {
        val connectionUri = "jdbc:postgresql://localhost:9987/my_database?user=john&password=doe"

        val configuration = URLParser.parse(connectionUri)
        assertThat(configuration.username).isEqualTo("john")
        assertThat(configuration.password).isEqualTo("doe")
        assertThat(configuration.database).isEqualTo("my_database")
        assertThat(configuration.host).isEqualTo("localhost")
        assertThat(configuration.port).isEqualTo(9987)
    }

    @Test
    fun `"postgresql URLParser" should     "create a connection from a heroku like URL , named server"`() {
        val connectionUri = "postgresql://john:doe@psql.heroku.com:9987/my_database"

        val configuration = URLParser.parse(connectionUri)
        assertThat(configuration.username).isEqualTo("john")
        assertThat(configuration.password).isEqualTo("doe")
        assertThat(configuration.database).isEqualTo("my_database")
        assertThat(configuration.host).isEqualTo("psql.heroku.com")
        assertThat(configuration.port).isEqualTo(9987)
    }

    @Test
    fun `"postgresql URLParser" should     "create a connection , the available fields and ipv6"`() {
        val connectionUri = "jdbc:postgresql://[::1]:9987/my_database?user=john&password=doe"

        val configuration = URLParser.parse(connectionUri)

        assertThat(configuration.username).isEqualTo("john")
        assertThat(configuration.password).isEqualTo("doe")
        assertThat(configuration.database).isEqualTo("my_database")
        assertThat(configuration.host).isEqualTo("::1")
        assertThat(configuration.port).isEqualTo(9987)
    }

    @Test
    fun `"postgresql URLParser" should     "create a connection from a heroku like URL and , ipv6"`() {
        val connectionUri = "postgresql://john:doe@[::1]:9987/my_database"

        val configuration = URLParser.parse(connectionUri)
        assertThat(configuration.username).isEqualTo("john")
        assertThat(configuration.password).isEqualTo("doe")
        assertThat(configuration.database).isEqualTo("my_database")
        assertThat(configuration.host).isEqualTo("::1")
        assertThat(configuration.port).isEqualTo(9987)
    }

    @Test
    fun `"postgresql URLParser" should     "create a connection , a missing hostname"`() {
        val connectionUri = "jdbc:postgresql:/my_database?user=john&password=doe"

        val configuration = URLParser.parse(connectionUri)

        assertThat(configuration.username).isEqualTo("john")
        assertThat(configuration.password).isEqualTo("doe")
        assertThat(configuration.database).isEqualTo("my_database")
        assertThat(configuration.host).isEqualTo("localhost")
        assertThat(configuration.port).isEqualTo(5432)
    }

    @Test
    fun `"postgresql URLParser" should     "create a connection , a missing database name"`() {
        val connectionUri = "jdbc:postgresql://[::1]:9987/?user=john&password=doe"

        val configuration = URLParser.parse(connectionUri)

        assertThat(configuration.username).isEqualTo("john")
        assertThat(configuration.password).isEqualTo("doe")
        assertThat(configuration.database).isEqualTo(null)
        assertThat(configuration.host).isEqualTo("::1")
        assertThat(configuration.port).isEqualTo(9987)
    }

    @Test
    fun `"postgresql URLParser" should     "create a connection , all default fields"`() {
        val connectionUri = "jdbc:postgresql:"

        val configuration = URLParser.parse(connectionUri)

        assertThat(configuration.username).isEqualTo("postgres")
        assertThat(configuration.password).isEqualTo(null)
        assertThat(configuration.database).isEqualTo(null)
        assertThat(configuration.host).isEqualTo("localhost")
        assertThat(configuration.port).isEqualTo(5432)
    }

    @Test
    fun `"postgresql URLParser" should     "create a connection , an empty (invalid) url"`() {
        val connectionUri = ""

        val configuration = URLParser.parse(connectionUri)

        assertThat(configuration.username).isEqualTo("postgres")
        assertThat(configuration.password).isEqualTo(null)
        assertThat(configuration.database).isEqualTo(null)
        assertThat(configuration.host).isEqualTo("localhost")
        assertThat(configuration.port).isEqualTo(5432)
    }


    @Test
    fun `"postgresql URLParser" should     "recognise a postgresql___ uri"`() {
        assertThat(URLParser.parse("postgresql://localhost:425/dbname")).isEqualTo(
            URLParser.DEFAULT.copy(
                username = "postgres",
                database = "dbname",
                port = 425,
                host = "localhost"
            )
        )
    }

    @Test
    fun `"postgresql URLParser" should     "recognise a postgres__ uri"`() {
        assertThat(URLParser.parse("postgres://localhost:425/dbname")).isEqualTo(
            URLParser.DEFAULT.copy(
                username = "postgres",
                database = "dbname",
                port = 425,
                host = "localhost"
            )
        )
    }

    @Test
    fun `"postgresql URLParser" should     "recognize a jdbc_postgresql___ uri"`() {
        assertThat(URLParser.parse("jdbc:postgresql://localhost:425/dbname")).isEqualTo(
            URLParser.DEFAULT.copy(
                username = "postgres",
                database = "dbname",
                port = 425,
                host = "localhost"
            )
        )
    }

    @Test
    fun `"postgresql URLParser" should     "pull the username and password from URI credentials"`() {
        assertThat(URLParser.parse("jdbc:postgresql://user:password@localhost:425/dbname")).isEqualTo(
            URLParser.DEFAULT.copy(
                username = "user",
                password = "password",
                database = "dbname",
                port = 425,
                host = "localhost"
            )
        )
    }

    @Test
    fun `"postgresql URLParser" should     "pull the username and password from query string"`() {
        assertThat(URLParser.parse("jdbc:postgresql://localhost:425/dbname?user=user&password=password")).isEqualTo(
            URLParser.DEFAULT.copy(
                username = "user",
                password = "password",
                database = "dbname",
                port = 425,
                host = "localhost"
            )
        )
    }

    // Included for consistency, so later changes aren't allowed to change behavior
    @Test
    fun `"postgresql URLParser" should     "use the query string parameters to override URI credentials"`() {
        assertThat(URLParser.parse("jdbc:postgresql://baduser:badpass@localhost:425/dbname?user=user&password=password")).isEqualTo(
            URLParser.DEFAULT.copy(
                username = "user",
                password = "password",
                database = "dbname",
                port = 425,
                host = "localhost"
            )
        )
    }

    @Test
    fun `"postgresql URLParser" should     "successfully default the port to the PostgreSQL port"`() {
        assertThat(URLParser.parse("jdbc:postgresql://baduser:badpass@localhost/dbname?user=user&password=password")).isEqualTo(
            URLParser.DEFAULT.copy(
                username = "user",
                password = "password",
                database = "dbname",
                port = 5432,
                host = "localhost"
            )
        )
    }

    @Test(expected = UnableToParseURLException::class)
    fun `"postgresql URLParser" should     "reject malformed ip addresses"`() {
        val connectionUri = "postgresql://john:doe@128.567.54.90:9987/my_database"

        val configuration = URLParser.parse(connectionUri)
        assertThat(configuration.username).isEqualTo("postgres")
        assertThat(configuration.password).isEqualTo(null)
        assertThat(configuration.database).isEqualTo(null)
        assertThat(configuration.host).isEqualTo("localhost")
        assertThat(configuration.port).isEqualTo(5432)

        URLParser.parseOrDie(connectionUri)
    }


}
