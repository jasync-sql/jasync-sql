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
        PostgreSQLColumnDecoderRegistry.Instance.registerType(Geom.GeometryColumnType, JtsColumnDecoder())
    }

    @Test
    fun `simple query`() {
        withHandler { handler ->
            val res1 = executeQuery(handler, "SELECT postgis_full_version()")
            assertThat(res1.rows[0][0].toString()).contains("POSTGIS=")
//            val res2 = executeQuery(handler, "SELECT ST_GeomFromText('POINT(1 2)',4326)")
            val res2 = executeQuery(handler, "SELECT ST_GeomFromText('LINESTRING(1 2, 3 4)',4326)")
            assertThat(res2.rows[0][0]).isEqualTo(WKTReader(GeometryFactory(PrecisionModel(), 4326)).read("LINESTRING(1 2, 3 4)"))
        }
    }
}
