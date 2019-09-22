package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.mysql.MySQLConnection
import io.r2dbc.spi.ConnectionMetadata

class JasyncConnectionMetadata(val jasyncConnection: Connection) :
    ConnectionMetadata {
    override fun getDatabaseVersion(): String {
        return (jasyncConnection as MySQLConnection).version().toString()
    }

    override fun getDatabaseProductName(): String {
        return MysqlConnectionFactoryProvider.MYSQL_DRIVER
    }

}
