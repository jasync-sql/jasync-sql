package com.oshai

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.pool.PoolConfiguration
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.pipeline.PipelineContext
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import java.util.concurrent.TimeUnit


fun main(args: Array<String>) {
    val server = embeddedServer(Netty, port = 8080) {
        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }

            routing {
                get("/") {
                    logger.info { "handling mysql request" }
                    handleMysqlRequest("select 0")
                }
            }
        }
    }
    println("STARTING")
    connectionPool.connect().get()
    try {
        server.start(wait = true)
    } finally {
        println("DISCO")
        connectionPool.disconnect().get()
    }
}

private val logger = KotlinLogging.logger {}

val configuration = Configuration(
        "mysql_async",
        "localhost",
        33306,
        "root",
        "mysql_async_tests")
val poolConfiguration = PoolConfiguration(
        maxObjects = 100,
        maxIdle = TimeUnit.MINUTES.toMillis(15),
        maxQueueSize = 10_000,
        validationInterval = TimeUnit.SECONDS.toMillis(30)
)
val connectionPool = ConnectionPool(factory = MySQLConnectionFactory(configuration), configuration = poolConfiguration)


private suspend fun PipelineContext<Unit, ApplicationCall>.handleMysqlRequest(query: String) {
    val queryResult = connectionPool.sendPreparedStatementAwait(query = query)
    call.respond(queryResult.rows!![0][0].toString())
}

private suspend fun Connection.sendPreparedStatementAwait(query: String, values: List<Any> = emptyList()): QueryResult =
        this.sendPreparedStatement(query, values).await()
