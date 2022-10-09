package com.github.jasync.r2dbc.mysql.integ

import com.github.jasync.r2dbc.mysql.JasyncConnectionFactory
import com.github.jasync.sql.db.mysql.MySQLConnection
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.util.FP
import io.mockk.mockk
import java.math.BigDecimal
import java.util.concurrent.CompletableFuture
import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.junit.Test
import reactor.core.publisher.Mono

class JasyncR2dbcIntegTest : R2dbcConnectionHelper() {

    @Test
    fun `simple r2dbc test`() {
        withConnection { c ->
            var rows = 0
            executeQuery(c, createTableNumericColumns)
            executeQuery(c, insertTableNumericColumns)
            val mycf = object : MySQLConnectionFactory(mockk()) {
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
                            Assertions.assertThat(rowMetadata.columnMetadatas.map { it.name }).isEqualTo(listOf("id",
                                "number_tinyint",
                                "number_smallint",
                                "number_mediumint",
                                "number_int",
                                "number_bigint",
                                "number_decimal",
                                "number_float",
                                "number_double"))
                        }
                }
                .doOnNext { rows++ }
                .subscribe()
            await.until { rows == 1 }
        }
    }

    @Test
    fun `reproduce ClassCastException`() {
        var rows = 0
        withConnection { c ->
            executeQuery(c, "CREATE TABLE holiday_model (" +
                "id INT NOT NULL AUTO_INCREMENT," +
                "name VARCHAR(45) NULL," +
                "description VARCHAR(255) NULL," +
                "locations LONGTEXT NULL," +
                "holidayDate DATETIME NULL," +
                "PRIMARY KEY (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci")
            executeQuery(c, "insert into holiday_model (name) values ('vacation')")
            val mycf = object : MySQLConnectionFactory(mockk()) {
                override fun create(): CompletableFuture<MySQLConnection> {
                    return FP.successful(c)
                }
            }
            val cf = JasyncConnectionFactory(mycf)
            Mono.from(cf.create())
                .flatMapMany { connection ->
                    connection
                        .createStatement("delete FROM holiday_model where id = 1")
                        .execute()
                }
                .doOnNext { rows++ }
                .subscribe()
            await.until { rows == 1 }
        }
    }
}
