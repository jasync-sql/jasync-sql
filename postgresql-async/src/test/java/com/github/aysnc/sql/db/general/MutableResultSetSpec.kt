package com.github.aysnc.sql.db.general

import com.github.jasync.sql.db.general.MutableResultSet
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.postgresql.column.ColumnTypes
import com.github.jasync.sql.db.postgresql.messages.backend.PostgreSQLColumnData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MutableResultSetSpec {

    private fun create(name: String, dataType: Int, columnNumber: Int = 0, dataTypeSize: Long = -1) =
        PostgreSQLColumnData(
            name = name,
            tableObjectId = 0,
            columnNumber = columnNumber,
            dataType = dataType,
            dataTypeSize = dataTypeSize,
            dataTypeModifier = 0,
            fieldFormat = 0
        )

    @Test
    fun `result set should correctly map column data to fields`() {

        val columns = listOf(
            create(
                name = "id",
                dataType = ColumnTypes.Integer,
                dataTypeSize = 4
            ),
            create(
                name = "name",
                columnNumber = 5,
                dataType = ColumnTypes.Varchar
            )
        )

        val text = "some data"
        val otherText = "some other data"

        val resultSet = MutableResultSet(columns)

        resultSet.addRow(listOf(1, text).toTypedArray())
        resultSet.addRow(listOf(2, otherText).toTypedArray())

        assertThat(resultSet(0)(0)).isEqualTo(1)
        assertThat(resultSet(0)("id")).isEqualTo(1)

        assertThat(resultSet(0)(1)).isEqualTo(text)
        assertThat(resultSet(0)("name")).isEqualTo(text)

        assertThat(resultSet(1)(0)).isEqualTo(2)
        assertThat(resultSet(1)("id")).isEqualTo(2)

        assertThat(resultSet(1)(1)).isEqualTo(otherText)
        assertThat(resultSet(1)("name")).isEqualTo(otherText)
    }

    @Test
    fun `result set should return the same order as the one given by columns`() {

        val columns = listOf(
            create("id", ColumnTypes.Integer),
            create("name", ColumnTypes.Varchar),
            create("birthday", ColumnTypes.Date),
            create("created_at", ColumnTypes.Timestamp),
            create("updated_at", ColumnTypes.Timestamp)
        )
        val resultSet = MutableResultSet(columns)

        assertThat(resultSet.columnNames()).isEqualTo(listOf("id", "name", "birthday", "created_at", "updated_at"))
    }
}
