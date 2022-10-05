package com.github.jasync.r2dbc.mysql.integ

import com.github.jasync.r2dbc.mysql.JasyncConnectionFactory
import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.util.FP
import io.mockk.mockk
import org.assertj.core.api.Assertions
import org.awaitility.Awaitility
import org.awaitility.kotlin.await
import org.awaitility.kotlin.untilCallTo
import org.junit.Assert
import org.junit.Test
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit


class JasyncR2dbcIntegTest : R2dbcConnectionHelper() {

    @Test
    fun `simple r2dbc test`() {
        withConnection { c ->
            var rows = 0
            executeQuery(c, createTableNumericColumns)
            executeQuery(c, insertTableNumericColumns)
            val mycf = object: MySQLConnectionFactory(mockk()) {
                override fun create(): CompletableFuture<MySQLConnection> {
                    return FP.successful(c)
                }
            }
            val cf = JasyncConnectionFactory(mycf)
            Mono.from(cf.create())
                .flatMapMany { connection ->
                    connection
                        .createStatement("SELECT * FROM numbers")
                        .execute()
                }
                .flatMap { result ->
                    result
                        .map { row, rowMetadata ->
                            Assertions.assertThat(row.get("number_tinyint") as Byte).isEqualTo(-100)
                            Assertions.assertThat(row.get("number_smallint") as Short).isEqualTo(32766)
                            Assertions.assertThat(row.get("number_mediumint") as Int).isEqualTo(8388607)
                            Assertions.assertThat(row.get("number_int") as Int).isEqualTo(2147483647)
                            Assertions.assertThat(row.get("number_bigint") as Long).isEqualTo(9223372036854775807L)
                            Assertions.assertThat(row.get("number_decimal")).isEqualTo(BigDecimal("450.764491"))
                            Assertions.assertThat(row.get("number_float")).isEqualTo(14.7F)
                            Assertions.assertThat(row.get("number_double")).isEqualTo(87650.9876)
                            Assertions.assertThat(rowMetadata.columnMetadatas.map { it.name }).isEqualTo(listOf(""))
                            Assert.fail()
                        }
                }
                .doOnNext { rows++ }
                .subscribe()
            await.until { rows == 1 }
        }
    }
}
