package com.github.jasync.sql.db.postgresql

import com.github.aysnc.sql.db.integration.ContainerHelper
import com.github.aysnc.sql.db.integration.DatabaseTestHelper
import com.github.jasync.sql.db.util.length
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class QuerySpec : DatabaseTestHelper() {
    val messagesCreate = """CREATE TEMP TABLE messages
                         (
                           id bigserial NOT NULL,
                           content character varying(255) NOT NULL,
                           CONSTRAINT bigserial_column_pkey PRIMARY KEY (id )
                         )"""

    @Test
    fun `connection should be able to select from a table`() {
        withHandler(ContainerHelper.defaultConfiguration.copy(currentSchema = "testing")) { handler ->
            executeDdl(handler, this.messagesCreate)
            executeDdl(handler, "INSERT INTO messages (content) VALUES ('Hello')")
        }

        withHandler { handler ->
            val rows = executePreparedStatement(handler, "SELECT id FROM testing.messages").rows
            assertThat(rows.length).isEqualTo(1)
        }
    }

}
