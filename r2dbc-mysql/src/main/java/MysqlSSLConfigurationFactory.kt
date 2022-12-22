package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.SSLConfiguration.Mode.Disable
import com.github.jasync.sql.db.SSLConfiguration.Mode.Prefer
import com.github.jasync.sql.db.SSLConfiguration.Mode.Require
import com.github.jasync.sql.db.SSLConfiguration.Mode.VerifyCA
import com.github.jasync.sql.db.SSLConfiguration.Mode.VerifyFull
import io.r2dbc.spi.ConnectionFactoryOptions
import io.r2dbc.spi.Option

object MysqlSSLConfigurationFactory {

    private val SSL_MODE_OPTION = Option.valueOf<String>("sslMode")
    private val SSL_MODE_MAP = mapOf(
        "disabled" to Disable,
        "preferred" to Prefer,
        "required" to Require,
        "verify_ca" to VerifyCA,
        "verify_identity" to VerifyFull
    )

    fun create(options: ConnectionFactoryOptions): SSLConfiguration {
        if (!options.hasOption(ConnectionFactoryOptions.SSL)) {
            return SSLConfiguration(mode = Disable)
        }
        if (!options.hasOption(SSL_MODE_OPTION)) {
            return SSLConfiguration(mode = Prefer)
        }
        val sslMode = options.getValue(SSL_MODE_OPTION) as String
        return SSLConfiguration(mode = SSL_MODE_MAP.getOrDefault(sslMode.lowercase(), Prefer))
    }
}
