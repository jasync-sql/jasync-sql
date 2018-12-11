package com.github.jasync.sql.db.general

import com.github.jasync.sql.db.RowData

class ArrayRowData(val row: Int, val mapping: Map<String, Int>, val columns: Array<Any?>) : RowData {

    private val columnsList by lazy { columns.toList() }

    override fun contains(element: Any?): Boolean = columns.contains(element)
    override fun containsAll(elements: Collection<Any?>): Boolean = columnsList.containsAll(elements)
    override fun indexOf(element: Any?): Int = columns.indexOf(element)
    override fun isEmpty(): Boolean = columns.isEmpty()
    override fun iterator(): Iterator<Any?> = columns.iterator()
    override fun lastIndexOf(element: Any?): Int = columns.lastIndexOf(element)
    override fun listIterator(): ListIterator<Any?> = columnsList.listIterator()
    override fun listIterator(index: Int): ListIterator<Any?> = columnsList.listIterator(index)
    override fun subList(fromIndex: Int, toIndex: Int): List<Any?> = columnsList.subList(fromIndex, toIndex)
    override val size: Int
        get() = columns.size

    /**
     *
     * Returns a column value by it's position in the originating query.
     *
     * @param index
     * @return
     */
    override fun get(index: Int): Any? = columns[index]

    /**
     *
     * Returns a column value by it's name in the originating query.
     *
     * @param column
     * @return
     */
    override fun get(column: String): Any? = columns[mapping.getValue(column)]

    /**
     *
     * Number of this row in the query results. Counts start at 0.
     *
     * @return
     */
    override fun rowNumber(): Int = row

    fun length(): Int = columns.size
}
