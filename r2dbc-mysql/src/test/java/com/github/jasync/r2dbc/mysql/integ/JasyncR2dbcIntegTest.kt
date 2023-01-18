package com.github.jasync.r2dbc.mysql.integ

import com.github.jasync.r2dbc.mysql.JasyncConnectionFactory
import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.util.FP
import io.mockk.mockk
import io.r2dbc.spi.Parameter
import io.r2dbc.spi.Result
import io.r2dbc.spi.Type
import mu.KotlinLogging
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.junit.Test
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture

private val logger = KotlinLogging.logger {}

class JasyncR2dbcIntegTest : R2dbcConnectionHelper() {

    @Test
    fun `simple r2dbc test`() {
        withConnection { c ->
            var rows = 0
            executeQuery(c, createTableNumericColumns)
            executeQuery(c, insertTableNumericColumns)
            val cf = createJasyncConnectionFactory(c)
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
                            Assertions.assertThat(rowMetadata.columnMetadatas.map { it.name }).isEqualTo(
                                listOf(
                                    "id",
                                    "number_tinyint",
                                    "number_smallint",
                                    "number_mediumint",
                                    "number_int",
                                    "number_bigint",
                                    "number_decimal",
                                    "number_float",
                                    "number_double"
                                )
                            )
                        }
                }
                .doOnNext { rows++ }
                .subscribe()
            await.until { rows == 1 }
        }
    }

    @Test
    fun `filter test`() {
        withConnection { c ->
            var filtering = 0
            var rows = 0
            executeQuery(c, createTable)
            executeQuery(c, """INSERT INTO users (name) VALUES ('Boogie Man'),('Dambeldor')""")
            val cf = createJasyncConnectionFactory(c)
            Mono.from(cf.create())
                .flatMapMany { connection ->
                    connection
                        .createStatement("SELECT name FROM users")
                        .execute()
                }
                .flatMap { result ->
                    result
                        // we test this function
                        .filter { segment ->
                            filtering++
                            segment is Result.RowSegment && segment.row().get("name") == "Dambeldor"
                        }.map { row, meta -> row.get(0) }
                }
                .doOnNext {
                    logger.info { "got row $it" }
                    rows++
                }
                .subscribe()
            await.until { filtering == 3 }
            await.until { rows == 1 }
        }
    }

    @Test
    fun `bind test`() {
        withConnection { c ->
            var rows = 0
            executeQuery(c, createTable)
            executeQuery(c, """INSERT INTO users (name) VALUES ('Boogie Man'),('Dambeldor')""")
            val cf = createJasyncConnectionFactory(c)
            Mono.from(cf.create())
                .flatMapMany { connection ->
                    connection
                        .createStatement("SELECT name FROM users where name in (?, ?)")
                        .bind(0, "Dambeldor")
                        .bind(1, "Boogie Man")
                        .execute()
                }
                .flatMap { result ->
                    result.map { row, rowMetadata ->
                        logger.info { "got row $row" }
                        rows++
                        row.get(0) as String
                    }
                }
                .doOnNext {
                    logger.info { "next row $it" }
                }
                .subscribe()
            await.until { rows == 2 }
        }
    }

    @Test
    fun `bind test for parametrized`() {
        withConnection { c ->
            var rows = 0
            executeQuery(c, createTable)
            executeQuery(c, """INSERT INTO users (name) VALUES ('Boogie Man'),('Dambeldor')""")
            val cf = createJasyncConnectionFactory(c)
            Mono.from(cf.create())
                .flatMapMany { connection ->
                    connection
                        .createStatement("SELECT name FROM users where name in (?)")
                        .bind(0, "Dambeldor".createParam())
                        .execute()
                }
                .flatMap { result ->
                    result.map { row, rowMetadata ->
                        logger.info { "got row $row" }
                        rows++
                        row.get(0) as String
                    }
                }
                .doOnNext {
                    logger.info { "next row $it" }
                }
                .subscribe()
            await.until { rows == 1 }
        }
    }

    private fun String.createParam(): Parameter = object : Parameter {
        override fun getType(): Type {
            TODO("Not implemented")
        }

        override fun getValue(): Any {
            return this@createParam
        }
    }

    private fun createJasyncConnectionFactory(c: MySQLConnection) =
        JasyncConnectionFactory(object : MySQLConnectionFactory(mockk()) {
            override fun create(): CompletableFuture<MySQLConnection> {
                return FP.successful(c)
            }
        })
}
