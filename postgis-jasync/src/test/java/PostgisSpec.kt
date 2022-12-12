package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.postgis.JtsColumnDecoder
import com.github.jasync.sql.db.postgresql.column.PostgreSQLColumnDecoderRegistry
import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.WKTReader

private val logger = KotlinLogging.logger {}

class PostgisSpec : DatabaseTestHelper() {

    private val lineString = WKTReader(GeometryFactory(PrecisionModel(), 4326)).read("LINESTRING(1 2, 3 4)")
    private val pointString = WKTReader(GeometryFactory(PrecisionModel(), 4326)).read("POINT(1 2)")

    private fun setup() {
        withHandler { handler ->
            val res = executeQuery(handler, "SELECT 'geometry'::regtype::oid")
            logger.info { "init geom type with res ${res.rows}" }
            PostgreSQLColumnDecoderRegistry.Instance.registerDecoder((res.rows[0][0] as Long).toInt(), JtsColumnDecoder())
        }
    }

    @Test
    fun `test version`() {
        withHandler { handler ->
            val res1 = executeQuery(handler, "SELECT postgis_full_version()")
            assertThat(res1.rows[0][0].toString()).contains("POSTGIS=")
        }
    }

    @Test
    fun `simple line query`() {
        setup()
        withHandler { handler ->
            val res = executeQuery(handler, "SELECT ST_GeomFromText('LINESTRING(1 2, 3 4)',4326)")
            assertThat(res.rows[0][0]).isEqualTo(lineString)
        }
    }

    @Test
    fun `simple point query`() {
        setup()
        withHandler { handler ->
            val res = executeQuery(handler, "SELECT ST_GeomFromText('POINT(1 2)',4326)")
            assertThat(res.rows[0][0]).isEqualTo(pointString)
        }
    }

    @Test
    fun `insert and query`() {
        setup()
        withHandler { handler ->
            executeQuery(handler, "DROP TABLE if exists postgis_geom_test")
            executeQuery(handler, "CREATE TABLE postgis_geom_test (geom geometry NOT NULL)")
            val insertRes = executePreparedStatement(handler, "insert into postgis_geom_test (geom) values (?)", listOf(lineString))
            assertThat(insertRes.rowsAffected).isEqualTo(1L)
            val res = executeQuery(handler, "SELECT geom from postgis_geom_test")
            val geom = res.rows[0][0]
            assertThat(geom).isEqualTo(lineString)
        }
    }
}
