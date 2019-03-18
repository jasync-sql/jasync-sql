package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.RowData
import io.r2dbc.spi.Row


class JasyncRow(private val rowData: RowData) : Row {


    override fun <T> get(identifier: Any, requestedType: Class<T>): T? {


        if (identifier is String) {
            return rowData.getAs(identifier)
        }

        if (identifier is Int) {
            return rowData.getAs(identifier)
        }

        throw IllegalArgumentException("Identifier must be a String or an Integer")
    }

    override fun get(identifier: Any): Any? {

        if (identifier is String) {
            return rowData.get(identifier)
        }

        if (identifier is Int) {
            return rowData.get(identifier)
        }

        throw IllegalArgumentException("Identifier must be a String or an Integer")
    }
}