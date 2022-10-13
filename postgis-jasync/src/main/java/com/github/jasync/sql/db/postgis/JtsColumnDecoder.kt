package com.github.jasync.sql.db.postgis

import com.github.jasync.sql.db.column.ColumnDecoder
import org.locationtech.jts.geom.Geometry

class JtsColumnDecoder : ColumnDecoder {

    private val parser = JtsBinaryParser()

    override fun decode(value: String): Geometry {
        return parser.parse(value)
    }
}
