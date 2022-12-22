package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.SSLConfiguration.Mode.Disable
import com.github.jasync.sql.db.SSLConfiguration.Mode.Prefer
import com.github.jasync.sql.db.SSLConfiguration.Mode.Require
import com.github.jasync.sql.db.SSLConfiguration.Mode.VerifyCA
import com.github.jasync.sql.db.SSLConfiguration.Mode.VerifyFull
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.ConnectionFactoryOptions.SSL
import io.r2dbc.spi.Option
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Parameterized::class)
internal class MysqlSSLConfigurationFactoryTest(
    private val options: ConnectionFactoryOptions,
    private val expectedSSLConfiguration: SSLConfiguration,
    private val message: String
) {

    companion object {

        private val SSL_MODE_OPTION = Option.valueOf<String>("sslMode")

        @JvmStatic
        @Parameterized.Parameters
        fun data() = listOf(
            createTestParams(
                options = ConnectionFactoryOptions.builder().build(),
                expected = SSLConfiguration(),
                message = "sslMode should be 'disabled' for non-secure protocol"
            ),
            createTestParams(
                options = ConnectionFactoryOptions.builder()
                    .option(SSL, true)
                    .build(),
                expected = SSLConfiguration(mode = Prefer),
                message = "sslMode should be 'preferred' by default"
            ),
            createTestParams(
                options = ConnectionFactoryOptions.builder()
                    .option(SSL, true)
                    .option(SSL_MODE_OPTION, "invalid")
                    .build(),
                expected = SSLConfiguration(mode = Prefer),
                message = "sslMode should be 'preferred' for invalid value"
            ),
            createTestParams(
                options = ConnectionFactoryOptions.builder()
                    .option(SSL, true)
                    .option(SSL_MODE_OPTION, "REQUIRED")
                    .build(),
                expected = SSLConfiguration(mode = Require),
                message = "sslMode should be case insensitive"
            ),
            createTestParams(
                options = ConnectionFactoryOptions.builder()
                    .option(SSL, true)
                    .option(SSL_MODE_OPTION, "disabled")
                    .build(),
                expected = SSLConfiguration(mode = Disable),
                message = "sslMode should be 'disabled'"
            ),
            createTestParams(
                options = ConnectionFactoryOptions.builder()
                    .option(SSL, true)
                    .option(SSL_MODE_OPTION, "preferred")
                    .build(),
                expected = SSLConfiguration(mode = Prefer),
                message = "sslMode should be 'preferred'"
            ),
            createTestParams(
                options = ConnectionFactoryOptions.builder()
                    .option(SSL, true)
                    .option(SSL_MODE_OPTION, "required")
                    .build(),
                expected = SSLConfiguration(mode = Require),
                message = "sslMode should be 'required'"
            ),
            createTestParams(
                options = ConnectionFactoryOptions.builder()
                    .option(SSL, true)
                    .option(SSL_MODE_OPTION, "verify_ca")
                    .build(),
                expected = SSLConfiguration(mode = VerifyCA),
                message = "sslMode should be 'verify_ca'"
            ),
            createTestParams(
                options = ConnectionFactoryOptions.builder()
                    .option(SSL, true)
                    .option(SSL_MODE_OPTION, "verify_identity")
                    .build(),
                expected = SSLConfiguration(mode = VerifyFull),
                message = "sslMode should be 'verify_identity'"
            ),
        )

        private fun createTestParams(
            options: ConnectionFactoryOptions,
            expected: SSLConfiguration,
            message: String
        ) = arrayOf(options, expected, message)
    }

    @Test
    fun shouldCreateProperSSLConfiguration() {
        // when
        val result = MysqlSSLConfigurationFactory.create(options)

        // then
        assertEquals(expectedSSLConfiguration, result, message)
    }
}
