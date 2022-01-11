package com.github.aysnc.sql.db.postgresql

import com.github.aysnc.sql.db.integration.ContainerHelper
import com.github.aysnc.sql.db.integration.DatabaseTestHelper
import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.postgresql.exceptions.GenericDatabaseException
import com.github.jasync.sql.db.util.length
import java.util.concurrent.ExecutionException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class QuerySpec : DatabaseTestHelper() {
    private val messagesCreate = """CREATE TABLE if not exists testing.messages
                         (
                           id bigserial NOT NULL,
                           content character varying(255) NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

    @Test
    fun `connection should be able to select from a table on search path`() {
        init()
        withHandler(ContainerHelper.defaultConfiguration.copy(currentSchema = "testing")) { handler ->
            executeQuery(handler, "INSERT INTO messages (content) VALUES ('Hello')")
        }

        withHandler { handler ->
            val rows = executePreparedStatement(handler, "SELECT id FROM testing.messages").rows
            assertThat(rows.length).isEqualTo(1)
        }
    }

    private fun init() {
        withHandler { handler ->
            executeDdl(handler, "create schema if not exists testing")
            executeDdl(handler, "drop table if exists testing.messages")
            executeDdl(handler, this.messagesCreate)
        }
    }

    @Test
    fun `error if table without using search path`() {
        init()
        withHandler(ContainerHelper.defaultConfiguration.copy(currentSchema = "testing")) { handler ->
            executeQuery(handler, "INSERT INTO messages (content) VALUES ('Hello')")
        }

        withHandler { handler ->
            val rows = executePreparedStatement(handler, "SELECT id FROM testing.messages").rows
            assertThat(rows.length).isEqualTo(1)

            verifyException(ExecutionException::class.java, GenericDatabaseException::class.java) {
                executePreparedStatement(handler, "SELECT id FROM messages")
            }
        }
    }
}
