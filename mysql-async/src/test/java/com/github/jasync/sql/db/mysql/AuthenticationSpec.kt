package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.SSLConfiguration.Mode
import com.github.jasync.sql.db.invoke
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.Test
import org.testcontainers.containers.BindMode
import org.testcontainers.containers.MySQLContainer
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

class AuthenticationSpec {

    @Test
    fun cachingSha2PasswordAuthentication() {
        val container = createContainer("mysql:8.0.31")

        withConnection(container, "root", "test") { connection ->
            connection.sendQuery("CREATE USER 'user1' IDENTIFIED WITH caching_sha2_password BY 'foo'").await()
            connection.sendQuery("GRANT ALL PRIVILEGES ON *.* to 'user1'").await()

            connection.sendQuery("CREATE USER 'user2' IDENTIFIED WITH caching_sha2_password BY 'bar'").await()
            connection.sendQuery("GRANT ALL PRIVILEGES ON *.* to 'user2'").await()
        }

        // First connection without SSL fails because we need to perform full authentication.
        assertThatThrownBy {
            withConnection(container, "user1", "foo") { /* Empty */ }
        }.hasRootCauseInstanceOf(IllegalStateException::class.java)
            .hasRootCauseMessage("Authentication is not possible over an unsafe connection. Please use SSL or specify 'rsaPublicKey'")

        // Perform full authentication with SSL.
        withConnection(container, "user1", "foo", SSL_MODE) { connection ->
            val result = connection.sendQuery(QUERY_CURRENT_PLUGIN).await()
            assertThat(result.rows).hasSize(1)
            assertThat(result.rows[0]("plugin")).isEqualTo("caching_sha2_password")
        }

        // Perform fast authentication without SSL.
        withConnection(container, "user1", "foo") { connection ->
            val result = connection.sendQuery(QUERY_CURRENT_PLUGIN).await()
            assertThat(result.rows).hasSize(1)
            assertThat(result.rows[0]("plugin")).isEqualTo("caching_sha2_password")
        }

        // Perform full authentication without SSL, with an RSA public key.
        withConnection(container, "user2", "bar", rsaPublicKey = PUBLIC_KEY) { connection ->
            val result = connection.sendQuery(QUERY_CURRENT_PLUGIN).await()
            assertThat(result.rows).hasSize(1)
            assertThat(result.rows[0]("plugin")).isEqualTo("caching_sha2_password")
        }

        // Perform fast authentication without SSL.
        withConnection(container, "user2", "bar") { connection ->
            val result = connection.sendQuery(QUERY_CURRENT_PLUGIN).await()
            assertThat(result.rows).hasSize(1)
            assertThat(result.rows[0]("plugin")).isEqualTo("caching_sha2_password")
        }

        container.stop()
    }

    @Test
    fun oldPasswordAuthentication() {
        val container = createContainer("mysql:5.6.51", "--skip-secure-auth")

        withConnection(container, "root", "test") { connection ->
            connection.sendQuery("CREATE USER 'user' IDENTIFIED WITH mysql_old_password").await()
            connection.sendQuery("SET old_passwords = 1").await()
            connection.sendQuery("SET PASSWORD FOR 'user' = PASSWORD('foo')").await()
            connection.sendQuery("GRANT ALL PRIVILEGES ON *.* to 'user'").await()
        }

        withConnection(container, "user", "foo") { connection ->
            val result = connection.sendQuery(QUERY_CURRENT_PLUGIN).await()
            assertThat(result.rows).hasSize(1)
            assertThat(result.rows[0]("plugin")).isEqualTo("mysql_old_password")
        }

        container.stop()
    }

    @Test
    fun nativePasswordAuthentication() {
        val container = createContainer("mysql:8.0.31")

        withConnection(container, "root", "test") { connection ->
            connection.sendQuery("CREATE USER 'user' IDENTIFIED WITH mysql_native_password BY 'foo'").await()
            connection.sendQuery("GRANT ALL PRIVILEGES ON *.* to 'user'").await()
        }

        withConnection(container, "user", "foo") { connection ->
            val result = connection.sendQuery(QUERY_CURRENT_PLUGIN).await()
            assertThat(result.rows).hasSize(1)
            assertThat(result.rows[0]("plugin")).isEqualTo("mysql_native_password")
        }

        container.stop()
    }

    @Test
    fun sha256Authentication() {
        val container = createContainer("mysql:5.7.32")

        withConnection(container, "root", "test") { connection ->
            connection.sendQuery("CREATE USER 'user' IDENTIFIED WITH sha256_password BY 'foo'").await()
            connection.sendQuery("GRANT ALL PRIVILEGES ON *.* to 'user'").await()
        }

        // We can send the plaintext password over SSL.
        withConnection(container, "user", "foo", SSL_MODE) { connection ->
            val result = connection.sendQuery(QUERY_CURRENT_PLUGIN).await()
            assertThat(result.rows).hasSize(1)
            assertThat(result.rows[0]("plugin")).isEqualTo("sha256_password")
        }

        // Prevent authentication over unsafe connections.
        assertThatThrownBy {
            withConnection(container, "user", "foo") { /* Empty */ }
        }.hasRootCauseInstanceOf(IllegalStateException::class.java)
            .hasRootCauseMessage("Authentication is not possible over an unsafe connection. Please use SSL or specify 'rsaPublicKey'")

        // But allow password encryption using an RSA public key.
        withConnection(container, "user", "foo", rsaPublicKey = PUBLIC_KEY) { connection ->
            val result = connection.sendQuery(QUERY_CURRENT_PLUGIN).await()
            assertThat(result.rows).hasSize(1)
            assertThat(result.rows[0]("plugin")).isEqualTo("sha256_password")
        }

        container.stop()
    }

    private fun createContainer(imageName: String, command: String? = null): MySQLContainer<*> {
        val container = MySQLContainer(imageName)
            .withUsername("root")
            .withPassword("test")

        if (command != null) {
            container.withCommand(command)
        }

        for (file in ContainerHelper.configurationFiles) {
            container.withClasspathResourceMapping(file, "/docker-entrypoint-initdb.d/$file", BindMode.READ_ONLY)
        }

        container.start()
        return container
    }

    private fun <T> withConnection(
        container: MySQLContainer<*>,
        username: String,
        password: String,
        sslConfiguration: SSLConfiguration = SSLConfiguration(Mode.Disable),
        rsaPublicKey: Path? = null,
        fn: (MySQLConnection) -> T,
    ): T {
        val configuration = Configuration(
            username = username,
            password = password,
            port = container.firstMappedPort,
            ssl = sslConfiguration,
            rsaPublicKey = rsaPublicKey,
        )

        val connection = MySQLConnection(configuration)
        connection.connect().await()

        val result = fn(connection)
        connection.close().await()

        return result
    }

    private fun <T> CompletableFuture<T>.await(): T {
        return this.get(10, TimeUnit.SECONDS)
    }

    private companion object {
        const val QUERY_CURRENT_PLUGIN = "SELECT plugin FROM mysql.user WHERE CURRENT_USER() = CONCAT(user, '@', host);"
        val SSL_MODE = SSLConfiguration(Mode.Prefer)
        val PUBLIC_KEY: Path = Paths.get(AuthenticationSpec::class.java.getResource("/public_key.pem")!!.toURI())
    }
}
