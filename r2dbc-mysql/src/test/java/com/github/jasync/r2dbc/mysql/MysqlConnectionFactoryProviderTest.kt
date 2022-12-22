package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.SSLConfiguration
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.verify
import io.r2dbc.spi.ConnectionFactoryOptions
import org.junit.Test

internal class MysqlConnectionFactoryProviderTest {

    private val provider = MysqlConnectionFactoryProvider()

    @Test
    fun shouldCreateMysqlConnectionWithMysqlSSLConfigurationFactory() {
        // given
        mockkObject(MysqlSSLConfigurationFactory)
        every { MysqlSSLConfigurationFactory.create(any()) } returns SSLConfiguration()

        val options =
            ConnectionFactoryOptions.parse("r2dbc:mysql://user@host:443/")

        // when
        provider.create(options)

        // then
        verify {
            MysqlSSLConfigurationFactory.create(options)
        }
    }
}
