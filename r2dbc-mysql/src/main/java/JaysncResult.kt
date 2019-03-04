package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.RowData
import io.r2dbc.spi.Result
import io.r2dbc.spi.RowMetadata
import io.reactivex.Flowable
import org.reactivestreams.Publisher
import java.util.function.BiFunction


class JaysncResult(private val resultSet: ResultSet) : Result {
    private val metadata = JasyncMetadata(resultSet)

    override fun getRowsUpdated(): Publisher<Int> {
        return Flowable.just(resultSet.size)
    }

    override fun <T> map(mappingFunction: BiFunction<io.r2dbc.spi.Row, RowMetadata, out T>): Publisher<T> {
        return Flowable.fromIterable<RowData>(resultSet)
            .map { mappingFunction.apply(JasyncRow(it), metadata) }
    }
}