package com.github.jasync.r2dbc.mysql.integ

import com.github.jasync.r2dbc.mysql.JasyncConnectionFactory
import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.util.FP
import io.github.oshai.kotlinlogging.KotlinLogging
import io.mockk.mockk
import io.r2dbc.pool.ConnectionPoolConfiguration
import io.r2dbc.spi.Parameter
import io.r2dbc.spi.Result
import io.r2dbc.spi.Type
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.hamcrest.core.IsEqual
import org.junit.Test
import org.springframework.r2dbc.connection.R2dbcTransactionManager
import org.springframework.r2dbc.connection.TransactionAwareConnectionFactoryProxy
import org.springframework.transaction.reactive.TransactionalOperator
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

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
                            assertThat(row.get("number_tinyint") as Byte).isEqualTo(-100)
                            assertThat(row.get("number_smallint") as Short).isEqualTo(32766)
                            assertThat(row.get("number_mediumint") as Int).isEqualTo(8388607)
                            assertThat(row.get("number_int") as Int).isEqualTo(2147483647)
                            assertThat(row.get("number_bigint") as Long).isEqualTo(9223372036854775807L)
                            assertThat(row.get("number_decimal")).isEqualTo(BigDecimal("450.764491"))
                            assertThat(row.get("number_float")).isEqualTo(14.7F)
                            assertThat(row.get("number_double")).isEqualTo(87650.9876)
                            assertThat(rowMetadata.columnMetadatas.map { it.name }).isEqualTo(
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
            executeQuery(c, createUserTable)
            executeQuery(c, insertUsers)
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
            executeQuery(c, createUserTable)
            executeQuery(c, insertUsers)
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
            executeQuery(c, createUserTable)
            executeQuery(c, insertUsers)
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

    @Test
    fun `join tables which have column with the same names test`() {
        withConnection { c ->
            var rows = 0
            executeQuery(c, createUserTable)
            executeQuery(c, insertUsers)
            executeQuery(c, createPostTable)
            executeQuery(c, insertPosts)
            val cf = createJasyncConnectionFactory(c)
            Mono.from(cf.create())
                .flatMapMany { connection ->
                    connection
                        .createStatement("SELECT * FROM users JOIN posts ON users.id = posts.user_id")
                        .execute()
                }
                .flatMap { result ->
                    result
                        .map { row, rowMetadata ->
                            assertThat(rowMetadata.columnMetadatas.map { it.name }).isEqualTo(
                                listOf(
                                    "id",
                                    "name",
                                    "id",
                                    "title",
                                    "user_id"
                                )
                            )
                        }
                }
                .doOnNext { rows++ }
                .subscribe()
            await.until { rows == 2 }
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

    @Test
    fun `r2dbc connection should be released on cancellation`() {
        val queryExecutionFlag = AtomicBoolean(false)
        val timeout = 3L
        val querySleepTime = 10L // should be greater than the above timeout intentionally in this TC
        val timeoutConfiguration = getConfiguration().copy(queryTimeout = Duration.ofSeconds(timeout))

        withConfigurableConnection(timeoutConfiguration) { c ->
            val mycf = object : MySQLConnectionFactory(mockk()) {
                override fun create(): CompletableFuture<MySQLConnection> {
                    return FP.successful(c)
                }
            }
            val cf = JasyncConnectionFactory(mycf)
            val r2dbcPoolConfig = ConnectionPoolConfiguration.builder()
                .initialSize(5)
                .minIdle(5)
                .connectionFactory(cf)
                .build()

            val r2dbcPool = io.r2dbc.pool.ConnectionPool(r2dbcPoolConfig)
            val tm = R2dbcTransactionManager(r2dbcPool)
            val to = TransactionalOperator.create(tm)

            val tcf = TransactionAwareConnectionFactoryProxy(r2dbcPool)

            val action = tcf.create()
                .flatMapMany { connection ->
                    connection.createStatement("SELECT SLEEP($querySleepTime)")
                        .execute()
                        .toMono()
                        .doOnSubscribe {
                            queryExecutionFlag.set(true)
                        }
                }
            val disposable = to.transactional(action).subscribe()
            await.untilAtomic(queryExecutionFlag, IsEqual(true))
            disposable.dispose()
            await.until {
                r2dbcPool.metrics.get().acquiredSize() == 0
            }
        }
    }
}
