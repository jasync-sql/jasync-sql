package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.ResultSet
import io.r2dbc.spi.Result
import io.r2dbc.spi.RowMetadata
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class JasyncResult(
    private val resultSet: ResultSet,
    private val rowsAffected: Long,
    private val selectLastInsertId: Boolean,
    private val lastInsertId: Long,
    private val generatedKeyName: String
) : Result {

    internal constructor(
        resultSet: ResultSet,
        rowsAffected: Long
    ) : this(resultSet, rowsAffected, false, 0, "")

    private val metadata = JasyncMetadata(resultSet)

    override fun getRowsUpdated(): Publisher<Int> {
        if (rowsAffected != 0L) {
            return Mono.just(rowsAffected.toInt())
        }
        return Mono.just(resultSet.size)
    }

    override fun <T> map(mappingFunction: BiFunction<io.r2dbc.spi.Row, RowMetadata, out T>): Publisher<T> {
        return if (selectLastInsertId) {
            Mono.fromSupplier { mappingFunction.apply(JasyncInsertSyntheticRow(generatedKeyName, lastInsertId), JasyncInsertSyntheticMetadata(generatedKeyName)) }
        } else {
            Flux.fromIterable(resultSet)
                .map { mappingFunction.apply(JasyncRow(it), metadata) }
        }
    }

    override fun filter(filter: Predicate<Result.Segment>): Result {
        TODO("Not yet implemented")
    }

    override fun <T : Any?> flatMap(mappingFunction: Function<Result.Segment, out Publisher<out T>>): Publisher<T> {
        TODO("Not yet implemented")
    }
}
