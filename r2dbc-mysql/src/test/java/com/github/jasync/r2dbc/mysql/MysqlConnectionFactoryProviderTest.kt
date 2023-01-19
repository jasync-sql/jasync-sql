package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.SSLConfiguration
import io.r2dbc.spi.ConnectionFactoryOptions
import org.junit.Assert.assertEquals
import org.junit.Test

class MysqlConnectionFactoryProviderTest {

    private val provider = MysqlConnectionFactoryProvider()

    @Test
    fun shouldCreateMysqlConnectionWithMysqlSSLConfigurationFactory() {

        val options =
            ConnectionFactoryOptions.parse("r2dbc:mysql://user@host:443/")

        // when
        val result = provider.create(options)

        // then
        assertEquals(SSLConfiguration(), result.mySQLConnectionFactory.configuration.ssl)
    }
}
