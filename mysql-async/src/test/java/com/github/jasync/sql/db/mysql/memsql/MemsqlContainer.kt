package com.github.jasync.sql.db.mysql.memsql

import com.github.jasync.sql.db.mysql.memsql.MemsqlContainerHelper.defaultMemsqlConfiguration
import org.testcontainers.containers.JdbcDatabaseContainer


class MemSQLContainer : JdbcDatabaseContainer<MemSQLContainer>("memsql/quickstart") {
    override fun getPassword(): String {
        return defaultMemsqlConfiguration.password.toString()
    }

    override fun getUsername(): String {
        return defaultMemsqlConfiguration.username
    }

    override fun getDatabaseName(): String {
        return defaultMemsqlConfiguration.database ?: throw NullPointerException("defaultConfiguration.database is null")
    }

    override fun getJdbcUrl(): String {
        return "jdbc:mysql://" + containerIpAddress + ":" + getMappedPort(defaultMemsqlConfiguration.port) + "/" + databaseName
    }

    override fun getTestQueryString(): String {
        return "select 1;"
    }

    override fun getDriverClassName(): String {
       return "com.mysql.jdbc.Driver"
    }

}
