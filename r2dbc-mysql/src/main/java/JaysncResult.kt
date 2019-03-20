package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.ResultSet
import com.github.jasync.sql.db.RowData
import io.r2dbc.spi.Result
import io.r2dbc.spi.RowMetadata
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.BiFunction


class JaysncResult(private val resultSet: ResultSet, private val rowsAffected: Long) : Result {
    private val metadata = JasyncMetadata(resultSet)

    override fun getRowsUpdated(): Publisher<Int> {
        if (rowsAffected != 0L) {
            return Mono.just(rowsAffected.toInt())
        }
        return Mono.just(resultSet.size)
    }

    override fun <T> map(mappingFunction: BiFunction<io.r2dbc.spi.Row, RowMetadata, out T>): Publisher<T> {
        return Flux.fromIterable<RowData>(resultSet)
                .map { mappingFunction.apply(JasyncRow(it), metadata) }
    }
}
