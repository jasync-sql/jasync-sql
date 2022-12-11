package com.github.aysnc.sql.db.integration

import com.github.jasync.sql.db.postgis.Geom
import com.github.jasync.sql.db.postgis.JtsColumnDecoder
import com.github.jasync.sql.db.postgresql.column.PostgreSQLColumnDecoderRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.io.WKTReader

class PostgisSpec : DatabaseTestHelper() {

    init {
        PostgreSQLColumnDecoderRegistry.Instance.registerDecoder(Geom.GeometryColumnType, JtsColumnDecoder())
        PostgreSQLColumnDecoderRegistry.Instance.registerDecoder(17995, JtsColumnDecoder())
    }

    private val lineString = WKTReader(GeometryFactory(PrecisionModel(), 4326)).read("LINESTRING(1 2, 3 4)")

    @Test
    fun `simple query`() {
        withHandler { handler ->
            val res1 = executeQuery(handler, "SELECT postgis_full_version()")
            assertThat(res1.rows[0][0].toString()).contains("POSTGIS=")
//            val res2 = executeQuery(handler, "SELECT ST_GeomFromText('POINT(1 2)',4326)")
            val res2 = executeQuery(handler, "SELECT ST_GeomFromText('LINESTRING(1 2, 3 4)',4326)")
            assertThat(res2.rows[0][0]).isEqualTo(lineString)
        }
    }

    @Test
    fun `insert and query`() {
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
