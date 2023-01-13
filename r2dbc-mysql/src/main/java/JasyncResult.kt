package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.ResultSet
import io.r2dbc.spi.Result
import io.r2dbc.spi.Result.RowSegment
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import org.reactivestreams.Publisher
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.function.BiFunction
import java.util.function.Function
import java.util.function.Predicate

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

    override fun getRowsUpdated(): Publisher<Long> {
        if (rowsAffected != 0L) {
            return Mono.just(rowsAffected)
        }
        return Mono.just(resultSet.size.toLong())
    }

    override fun <T> map(mappingFunction: BiFunction<io.r2dbc.spi.Row, RowMetadata, out T>): Publisher<T> {
        return if (selectLastInsertId) {
            Mono.fromSupplier {
                mappingFunction.apply(
                    JasyncInsertSyntheticRow(generatedKeyName, lastInsertId),
                    JasyncInsertSyntheticMetadata(generatedKeyName)
                )
            }
        } else {
            Flux.fromIterable(resultSet)
                .map { mappingFunction.apply(JasyncRow(it, metadata), metadata) }
        }
    }

    override fun filter(filter: Predicate<Result.Segment>): Result {
        return JasyncSegmentResult(this).filter(filter)
    }

    override fun <T : Any?> flatMap(mappingFunction: Function<Result.Segment, out Publisher<out T>>): Publisher<T> {
        return JasyncSegmentResult(this).flatMap(mappingFunction)
    }

    class JasyncSegmentResult private constructor(
        private val segments: Flux<Result.Segment>,
        private val result: JasyncResult
    ) : Result {
        constructor(result: JasyncResult) : this(
            Flux.concat(
                Flux.fromIterable(result.resultSet)
                    .map { JasyncRow(it, result.metadata) },
                Flux.just(Result.UpdateCount { result.rowsAffected })
            ),
            result
        )

        override fun getRowsUpdated(): Publisher<Long> {
            return result.rowsUpdated
        }

        override fun <T : Any?> map(mappingFunction: BiFunction<Row, RowMetadata, out T>): Publisher<T> {
            return segments
                .handle { segment, sink ->
                    if (segment is RowSegment) {
                        sink.next(mappingFunction.apply(segment.row(), segment.row().metadata))
                    }
                }
        }

        override fun filter(filter: Predicate<Result.Segment>): Result {
            return JasyncSegmentResult(segments.filter(filter), result)
        }

        override fun <T : Any?> flatMap(mappingFunction: Function<Result.Segment, out Publisher<out T>>): Publisher<T> {
            return segments.concatMap { segment: Result.Segment ->
                mappingFunction.apply(segment)
            }
        }
    }
}
