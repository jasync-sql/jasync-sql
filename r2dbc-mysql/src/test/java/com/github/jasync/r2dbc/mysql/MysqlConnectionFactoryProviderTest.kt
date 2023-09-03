package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.SSLConfiguration
import io.r2dbc.spi.ConnectionFactoryOptions
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class MysqlConnectionFactoryProviderTest {

    private val provider = MysqlConnectionFactoryProvider()

    @Test
    fun shouldCreateMysqlConnectionWithMysqlSSLConfigurationFactory() {
        val options = ConnectionFactoryOptions.parse("r2dbc:mysql://user@host:443/")

        // when
        val result = provider.create(options)

        // then
        assertEquals(SSLConfiguration(), result.mySQLConnectionFactory.configuration.ssl)
    }

    @Test
    fun shouldUseDefaultPortWhenPortIsNotSpecified() {
        val options = ConnectionFactoryOptions.parse("r2dbc:mysql://user@host/")

        // when
        val result = provider.create(options)

        // then
        assertEquals(3306, result.mySQLConnectionFactory.configuration.port)
    }

    @Test
    fun shouldUseSpecifiedPort() {
        val options = ConnectionFactoryOptions.parse("r2dbc:mysql://user@host:3307/")

        // when
        val result = provider.create(options)

        // then
        assertEquals(3307, result.mySQLConnectionFactory.configuration.port)
    }

    @Test
    fun shouldNotUseWhenRsaPublicKeyIsNotSpecified() {
        val options = ConnectionFactoryOptions.parse("r2dbc:mysql://user@host/")

        // when
        val result = provider.create(options)

        // then
        assertEquals(null, result.mySQLConnectionFactory.configuration.rsaPublicKey)
    }

    @Test
    fun shouldUseSpecifiedRsaPublicKey() {
        val options = ConnectionFactoryOptions.parse("r2dbc:mysql://user@host/db?serverRSAPublicKeyFile=rsa.pem")

        // when
        val result = provider.create(options)

        // then
        assertEquals("rsa.pem", result.mySQLConnectionFactory.configuration.rsaPublicKey.toString())
    }

    @Test
    fun shouldUseTimeoutAsString() {
        val options = ConnectionFactoryOptions.parse("r2dbc:mysql://user@host/db?connectTimeout=PT3S")

        // when
        val result = provider.create(options)

        // then
        assertEquals(Duration.parse("PT3S").toMillis().toInt(), result.mySQLConnectionFactory.configuration.connectionTimeout)
    }
}
