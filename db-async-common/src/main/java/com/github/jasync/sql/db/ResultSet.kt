package com.github.jasync.sql.db

/**
 *
 * Represents the collection of rows that is returned from a statement inside a {@link QueryResult}. It's basically
 * a collection of Array<Any>. Mutating fields in this array will not affect the database in any way
 *
 */

interface ResultSet : List<RowData> {

    /**
     *
     * The names of the columns returned by the statement.
     *
     * @return
     */

    fun columnNames(): List<String>
}

operator fun ResultSet.invoke(index: Int): RowData = this[index]

val EMPTY_RESULT_SET: ResultSet = object : ResultSet, List<RowData> by emptyList() {
    override fun columnNames(): List<String> = emptyList()
}
