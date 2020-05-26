package com.github.jasync.sql.db.mysql.util

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.exceptions.UnableToParseURLException
import com.github.jasync.sql.db.mysql.util.URLParser.parse
import com.github.jasync.sql.db.mysql.util.URLParser.parseOrDie
import io.netty.buffer.PooledByteBufAllocator
import io.netty.util.CharsetUtil
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import org.junit.Test

class URLParserSpec {

    @Test
    fun `have a reasonable default`() {
        val c = Configuration("root")
        assertEquals("root", c.username)
        assertEquals("localhost", c.host)
        assertEquals(5432, c.port)
        assertNull(c.password)
        assertNull(c.database)
        assertEquals(c.ssl, SSLConfiguration())
        assertEquals(CharsetUtil.UTF_8, c.charset)
        assertEquals(16777216, c.maximumMessageSize)
        assertEquals(PooledByteBufAllocator.DEFAULT, c.allocator)
        assertNull(c.queryTimeout)
    }

    // Divided into sections
    // ========,jdbc:mysql ===========
    @Test
    fun `create a jdbc mysql connection with the available fields`() {
        val connectionUri = "jdbc:mysql://128.167.54.90:9987/my_database?user=john&password=doe"

        assertEquals(
            parse(connectionUri), Configuration(
                username = "john",
                password = "doe",
                database = "my_database",
                host = "128.167.54.90",
                port = 9987
            )
        )
    }

    @Test
    fun `create a connection without port`() {
        val connectionUri = "jdbc:mysql://128.167.54.90/my_database?user=john&password=doe"

        assertEquals(
            parse(connectionUri), URLParser.DEFAULT.copy(
                username = "john",
                password = "doe",
                database = "my_database",
                host = "128.167.54.90"
            )
        )
    }

    @Test
    fun `create a connection without username and password`() {
        val connectionUri = "jdbc:mysql://128.167.54.90:9987/my_database"

        assertEquals(
            parse(connectionUri), URLParser.DEFAULT.copy(
                database = "my_database",
                host = "128.167.54.90",
                port = 9987
            )
        )
    }

    @Test
    fun `create a connection from a heroku like URL using 'mysql' protocol`() {
        val connectionUri = "mysql://john:doe@128.167.54.90:9987/my_database"

        assertEquals(
            parse(connectionUri), Configuration(
                username = "john",
                password = "doe",
                database = "my_database",
                host = "128.167.54.90",
                port = 9987
            )
        )
    }

    @Test
    fun `create a connection with the available fields and named server`() {
        val connectionUri = "jdbc:mysql://localhost:9987/my_database?user=john&password=doe"

        assertEquals(
            parse(connectionUri), Configuration(
                username = "john",
                password = "doe",
                database = "my_database",
                host = "localhost",
                port = 9987
            )
        )
    }

    @Test
    fun `create a connection from a heroku like URL with named server`() {
        val connectionUri = "mysql://john:doe@psql.heroku.com:9987/my_database"

        val configuration = parse(connectionUri)
        assertEquals("john", configuration.username)
        assertEquals("doe", configuration.password)
        assertEquals("my_database", configuration.database)
        assertEquals("psql.heroku.com", configuration.host)
        assertEquals(9987, configuration.port)
    }

    @Test
    fun `create a connection with the available fields and ipv6`() {
        val connectionUri = "jdbc:mysql://[::1]:9987/my_database?user=john&password=doe"

        val configuration = parse(connectionUri)

        assertEquals(configuration.username, "john")
        assertEquals(configuration.password, "doe")
        assertEquals(configuration.database, "my_database")
        assertEquals(configuration.host, "::1")
        assertEquals(configuration.port, 9987)
    }

    @Test
    fun `create a connection from a heroku like URL and with ipv6`() {
        val connectionUri = "mysql://john:doe@[::1]:9987/my_database"

        val configuration = parse(connectionUri)
        assertEquals(configuration.username, "john")
        assertEquals(configuration.password, "doe")
        assertEquals(configuration.database, "my_database")
        assertEquals(configuration.host, "::1")
        assertEquals(configuration.port, 9987)
    }

    @Test
    fun `create a connection with a missing hostname`() {
        val connectionUri = "jdbc:mysql:/my_database?user=john&password=doe"

        assertEquals(
            parse(connectionUri), URLParser.DEFAULT.copy(
                username = "john",
                password = "doe",
                database = "my_database"
            )
        )
    }

    @Test
    fun `create a connection with a missing database name`() {
        val connectionUri = "jdbc:mysql://[::1]:9987/?user=john&password=doe"

        val configuration = parse(connectionUri)

        assertEquals(configuration.username, "john")
        assertEquals(configuration.password, "doe")
        assertNull(configuration.database)
        assertEquals(configuration.host, "::1")
        assertEquals(configuration.port, 9987)
    }

    @Test
    fun `create a connection with all default fields`() {
        val connectionUri = "jdbc:mysql:"

        val configuration = parse(connectionUri)

        assertEquals(configuration.username, "root")
        assertNull(configuration.password)
        assertNull(configuration.database)
        assertEquals(configuration.host, "127.0.0.1")
        assertEquals(configuration.port, 3306)
    }

    @Test
    fun `create a connection with an empty (invalid) url`() {
        val connectionUri = ""

        val configuration = parse(connectionUri)

        assertEquals(configuration.username, "root")
        assertNull(configuration.password)
        assertNull(configuration.database)
        assertEquals(configuration.host, "127.0.0.1")
        assertEquals(configuration.port, 3306)
    }

    @Test
    fun `recognise a mysql uri`() {
        assertEquals(
            parse("mysql://localhost:425/dbname"), Configuration(
                username = "root",
                database = "dbname",
                port = 425,
                host = "localhost"
            )
        )
    }

    @Test
    fun `recognize a jdbc mysql uri`() {
        assertEquals(
            parse("jdbc:mysql://localhost:425/dbname"), Configuration(
                username = "root",
                database = "dbname",
                port = 425,
                host = "localhost"
            )
        )
    }

    @Test
    fun `pull the username and password from URI credentials`() {
        assertEquals(
            parse("jdbc:mysql://user:password@localhost:425/dbname"), Configuration(
                username = "user",
                password = "password",
                database = "dbname",
                port = 425,
                host = "localhost"
            )
        )
    }

    @Test
    fun `pull the username and password from query string`() {
        assertEquals(
            parse("jdbc:mysql://localhost:425/dbname?user=user&password=password"), Configuration(
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
    fun `use the query string parameters to override URI credentials`() {
        assertEquals(
            parse("jdbc:mysql://baduser:badpass@localhost:425/dbname?user=user&password=password"), Configuration(
                username = "user",
                password = "password",
                database = "dbname",
                port = 425,
                host = "localhost"
            )
        )
    }

    @Test
    fun `successfully default the port to the mysql port`() {
        assertEquals(
            parse("jdbc:mysql://baduser:badpass@localhost/dbname?user=user&password=password"), Configuration(
                username = "user",
                password = "password",
                database = "dbname",
                port = 3306,
                host = "localhost"
            )
        )
    }

    @Test
    fun `reject malformed ip addresses`() {
        val connectionUri = "mysql://john:doe@128.567.54.90:9987/my_database"

        val configuration = parse(connectionUri)
        assertEquals(configuration.username, "root")
        assertNull(configuration.password)
        assertNull(configuration.database)
        assertEquals(configuration.host, "127.0.0.1")
        assertEquals(configuration.port, 3306)
        try {
            parseOrDie(connectionUri)
        } catch (e: UnableToParseURLException) {
            return
        }
        assertFalse(true, "UnabletoParse is not thrown")
    }
}
