package com.oshai

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.mysql.MySQLConnectionBuilder
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.util.pipeline.PipelineContext
import kotlinx.coroutines.future.await
import mu.KotlinLogging
import java.util.concurrent.TimeUnit


fun main() {
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

val connectionPool = MySQLConnectionBuilder.createConnectionPool {
    username = "test"
    host = "localhost"
    port = 3306
    password = "123456"
    database = "test"
    maxActiveConnections = 100
    maxIdleTime = TimeUnit.MINUTES.toMillis(15)
    maxPendingQueries = 10_000
    connectionValidationInterval = TimeUnit.SECONDS.toMillis(30)

}


private suspend fun PipelineContext<Unit, ApplicationCall>.handleMysqlRequest(query: String) {
    val queryResult = connectionPool.sendPreparedStatementAwait(query = query)
    call.respond(queryResult.rows[0][0].toString())
}

private suspend fun Connection.sendPreparedStatementAwait(query: String, values: List<Any> = emptyList()): QueryResult =
    this.sendPreparedStatement(query, values).await()
