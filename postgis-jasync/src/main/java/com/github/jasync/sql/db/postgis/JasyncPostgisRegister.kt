package com.github.jasync.sql.db.postgis

import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.postgresql.column.PostgreSQLColumnDecoderRegistry
import com.github.jasync.sql.db.util.FP
import com.github.jasync.sql.db.util.map
import mu.KotlinLogging
import java.util.concurrent.CompletableFuture
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * This class is responsible to register the geometry type decoder.
 * To use it call init() method and wait for future completion.
 * Since the geometry type is part of an extension and has dynamic oid it requires a connection to query the db for
 * the actual oid.
 */
object JasyncPostgisRegister {

    val geometryRegistered = AtomicBoolean(false)

    @JvmStatic
    fun init(connection: Connection): CompletableFuture<Unit> {
        if (geometryRegistered.get()) {
            logger.trace { "init geometry type already registered" }
            return FP.successful(Unit)
        }
        return connection.sendQuery("SELECT 'geometry'::regtype::oid").map { result ->
            val key = (result.rows[0][0] as Long).toInt()
            logger.info { "init geometry type with id $key" }
            PostgreSQLColumnDecoderRegistry.Instance.registerDecoder(key, JtsColumnDecoder())
            geometryRegistered.set(true)
        }
    }
}
