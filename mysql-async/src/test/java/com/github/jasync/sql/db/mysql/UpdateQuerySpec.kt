package com.github.jasync.sql.db.mysql

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UpdateQuerySpec : ConnectionHelper() {

    @Test
    fun `test rowsAffected on update with CLIENT_FOUND_ROWS capability should return all filtered rows and not just actual change`() {
        val tableName = "users_${System.currentTimeMillis()}"
        System.getProperties().remove(MySQLConnection.CLIENT_FOUND_ROWS_PROP_NAME)
        try {
            val createTable = """CREATE TABLE $tableName (
                              id INT NOT NULL AUTO_INCREMENT ,
                              name VARCHAR(255) CHARACTER SET 'utf8mb4' NOT NULL ,
                              PRIMARY KEY (id) );"""
            val insert = """INSERT INTO $tableName (name) VALUES ('Boogie Man')"""
            val query = "update $tableName set name = 'Boogie Man' where name = 'Boogie Man'"
            withConnection { connection ->
                assertThat(executeQuery(connection, createTable).rowsAffected).isEqualTo(0)
                assertThat(executeQuery(connection, insert).rowsAffected).isEqualTo(1)
                val result = executeQuery(connection, query)
                assertThat(result.rowsAffected).isEqualTo(0L)
            }
            System.setProperty(MySQLConnection.CLIENT_FOUND_ROWS_PROP_NAME, "true")
            withConnection { connection ->
                val resultWithProp = executeQuery(connection, query)
                assertThat(resultWithProp.rowsAffected).isEqualTo(1L)
            }
        } finally {
            System.getProperties().remove(MySQLConnection.CLIENT_FOUND_ROWS_PROP_NAME)
        }
    }
}
