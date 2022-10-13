package com.github.jasync.sql.db.postgis

import net.postgis.jdbc.geometry.binary.ByteGetter
import net.postgis.jdbc.geometry.binary.ValueGetter
import org.locationtech.jts.geom.Coordinate
import org.locationtech.jts.geom.CoordinateSequence
import org.locationtech.jts.geom.Geometry
import org.locationtech.jts.geom.GeometryCollection
import org.locationtech.jts.geom.GeometryFactory
import org.locationtech.jts.geom.LineString
import org.locationtech.jts.geom.LinearRing
import org.locationtech.jts.geom.MultiLineString
import org.locationtech.jts.geom.MultiPoint
import org.locationtech.jts.geom.MultiPolygon
import org.locationtech.jts.geom.Point
import org.locationtech.jts.geom.Polygon
import org.locationtech.jts.geom.PrecisionModel
import org.locationtech.jts.geom.impl.PackedCoordinateSequence

/**
 * Parse binary representation of geometries. Currently, only text rep (hexed)
 * implementation is tested.
 *
 * It should be easy to add char[] and CharSequence ByteGetter instances,
 * although the latter one is not compatible with older jdks.
 *
 * I did not implement real unsigned 32-bit integers or emulate them with long,
 * as both java Arrays and Strings currently can have only 2^31-1 elements
 * (bytes), so we cannot even get or build Geometries with more than approx.
 * 2^28 coordinates (8 bytes each).
 *
 */
@Suppress("UNCHECKED_CAST", "IMPLICIT_NOTHING_TYPE_ARGUMENT_IN_RETURN_POSITION", "UNREACHABLE_CODE")
class JtsBinaryParser {
    private val jtsFactory: GeometryFactory = GeometryFactory(PrecisionModel(), 4326)

    /**
     * Parse a hex encoded geometry
     * @param value String containing the hex data to be parsed
     * @return the resulting parsed geometry
     */
    fun parse(value: String?): Geometry {
        val bytes = ByteGetter.StringByteGetter(value)
        return parseGeometry(valueGetterForEndian(bytes))
    }

    /**
     * Parse a binary encoded geometry.
     * @param value byte array containing the binary encoded geometru
     * @return the resulting parsed geometry
     */
    fun parse(value: ByteArray?): Geometry {
        val bytes = ByteGetter.BinaryByteGetter(value)
        return parseGeometry(valueGetterForEndian(bytes))
    }
    /**
     * Parse with a known geometry factory
     * @param data ValueGetter for the data to be parsed
     * @param srid the SRID to be used for parsing
     * @param inheritSrid flag to toggle inheriting SRIDs
     * @return The resulting Geometry
     */
    /**
     * Parse a geometry starting at offset.
     * @param data ValueGetter for the data to be parsed
     * @return The resulting Geometry
     */
    private fun <T : Geometry> parseGeometry(data: ValueGetter, srid: Int = 0, inheritSrid: Boolean = false): T {
        var sridVar = srid
        val endian = data.byte // skip and test endian flag
        require(endian == data.endian) { "Endian inconsistency!" }
        val typeword = data.int
        val realtype = typeword and 0x1FFFFFFF // cut off high flag bits
        val haveZ = typeword and -0x80000000 != 0
        val haveM = typeword and 0x40000000 != 0
        val haveS = typeword and 0x20000000 != 0
        if (haveS) {
            val newsrid = Geom.parseSRID(data.int)
            sridVar = if (inheritSrid && newsrid != sridVar) {
                throw IllegalArgumentException("Inconsistent srids in complex geometry: $sridVar, $newsrid")
            } else {
                newsrid
            }
        } else if (!inheritSrid) {
            sridVar = Geom.UNKNOWN_SRID
        }
        val result: Geometry = when (realtype) {
            Geom.POINT -> parsePoint(data, haveZ, haveM)
            Geom.LINESTRING -> parseLineString(data, haveZ, haveM)
            Geom.POLYGON -> parsePolygon(data, haveZ, haveM, sridVar)
            Geom.MULTIPOINT -> parseMultiPoint(data, sridVar)
            Geom.MULTILINESTRING -> parseMultiLineString(data, sridVar)
            Geom.MULTIPOLYGON -> parseMultiPolygon(data, sridVar)
            Geom.GEOMETRYCOLLECTION -> parseCollection(data, sridVar)
            else -> throw IllegalArgumentException("Unknown Geometry Type!")
        }
        result.srid = sridVar
        return result as T
    }

    private fun parsePoint(data: ValueGetter, haveZ: Boolean, haveM: Boolean): Point {
        val X = data.double
        val Y = data.double
        val result: Point
        result = if (haveZ) {
            val Z = data.double
            jtsFactory.createPoint(Coordinate(X, Y, Z))
        } else {
            jtsFactory.createPoint(Coordinate(X, Y))
        }
        if (haveM) { // skip M value
            data.double
        }
        return result
    }

    /** Parse an Array of "full" Geometries  */
    private fun parseGeometryArray(data: ValueGetter, container: Array<out Geometry?>, srid: Int) {
        for (i in container.indices) {
            container[i] = parseGeometry(data, srid, true)
        }
    }

    /**
     * Parse an Array of "slim" Points (without endianness and type, part of
     * LinearRing and Linestring, but not MultiPoint!
     *
     * @param haveZ
     * @param haveM
     */
    private fun parseCS(data: ValueGetter, haveZ: Boolean, haveM: Boolean): CoordinateSequence {
        val count = data.int
        val dims = if (haveZ) 3 else 2
        val cs: CoordinateSequence = PackedCoordinateSequence.Double(count, dims, 0)
        for (i in 0 until count) {
            for (d in 0 until dims) {
                cs.setOrdinate(i, d, data.double)
            }
            if (haveM) { // skip M value
                data.double
            }
        }
        return cs
    }

    private fun parseMultiPoint(data: ValueGetter, srid: Int): MultiPoint {
        val points = arrayOfNulls<Point>(data.int)
        parseGeometryArray(data, points, srid)
        return jtsFactory.createMultiPoint(points)
    }

    private fun parseLineString(data: ValueGetter, haveZ: Boolean, haveM: Boolean): LineString {
        return jtsFactory.createLineString(parseCS(data, haveZ, haveM))
    }

    private fun parseLinearRing(data: ValueGetter, haveZ: Boolean, haveM: Boolean): LinearRing {
        return jtsFactory.createLinearRing(parseCS(data, haveZ, haveM))
    }

    private fun parsePolygon(data: ValueGetter, haveZ: Boolean, haveM: Boolean, srid: Int): Polygon {
        val holecount = data.int - 1
        val rings = arrayOfNulls<LinearRing>(holecount)
        val shell = parseLinearRing(data, haveZ, haveM)
        shell.srid = srid
        for (i in 0 until holecount) {
            rings[i] = parseLinearRing(data, haveZ, haveM)
            rings[i]!!.srid = srid
        }
        return jtsFactory.createPolygon(shell, rings)
    }

    private fun parseMultiLineString(data: ValueGetter, srid: Int): MultiLineString {
        val count = data.int
        val strings = arrayOfNulls<LineString>(count)
        parseGeometryArray(data, strings, srid)
        return jtsFactory.createMultiLineString(strings)
    }

    private fun parseMultiPolygon(data: ValueGetter, srid: Int): MultiPolygon {
        val count = data.int
        val polys = arrayOfNulls<Polygon>(count)
        parseGeometryArray(data, polys, srid)
        return jtsFactory.createMultiPolygon(polys)
    }

    private fun parseCollection(data: ValueGetter, srid: Int): GeometryCollection {
        val count = data.int
        val geoms = arrayOfNulls<Geometry>(count)
        parseGeometryArray(data, geoms, srid)
        return jtsFactory.createGeometryCollection(geoms)
    }

    companion object {
        /**
         * Get the appropriate ValueGetter for my endianness
         *
         * @param bytes
         * The appropriate Byte Getter
         *
         * @return the ValueGetter
         */
        fun valueGetterForEndian(bytes: ByteGetter): ValueGetter {
            return if (bytes[0] == ValueGetter.XDR.NUMBER.toInt()) { // XDR
                ValueGetter.XDR(bytes)
            } else if (bytes[0] == ValueGetter.NDR.NUMBER.toInt()) {
                ValueGetter.NDR(bytes)
            } else {
                throw IllegalArgumentException("Unknown Endian type:" + bytes[0])
            }
        }
    }
}

object Geom {

    // OpenGIS Geometry types as defined in the OGC WKB Spec
    // (May we replace this with an ENUM as soon as JDK 1.5
    // has gained widespread usage?)
    /** Fake type for linear ring  */
    const val LINEARRING = 0

    /**
     * The OGIS geometry type number for points.
     */
    const val POINT = 1

    /**
     * The OGIS geometry type number for lines.
     */
    const val LINESTRING = 2

    /**
     * The OGIS geometry type number for polygons.
     */
    const val POLYGON = 3

    /**
     * The OGIS geometry type number for aggregate points.
     */
    const val MULTIPOINT = 4

    /**
     * The OGIS geometry type number for aggregate lines.
     */
    const val MULTILINESTRING = 5

    /**
     * The OGIS geometry type number for aggregate polygons.
     */
    const val MULTIPOLYGON = 6

    /**
     * The OGIS geometry type number for feature collections.
     */
    const val GEOMETRYCOLLECTION = 7
    val ALLTYPES = arrayOf(
        "", // internally used LinearRing does not have any text in front of
        // it
        "POINT", "LINESTRING", "POLYGON", "MULTIPOINT", "MULTILINESTRING",
        "MULTIPOLYGON", "GEOMETRYCOLLECTION"
    )

    /**
     * The Text representations of the geometry types
     *
     * @param type int value of the type to lookup
     * @return String reprentation of the type.
     */
    fun getTypeString(type: Int): String {
        return if (type >= 0 && type <= 7) {
            ALLTYPES[type]
        } else {
            throw IllegalArgumentException("Unknown Geometry type$type")
        }
    }

    /**
     * Official UNKNOWN srid value
     */
    const val UNKNOWN_SRID = 0

    /**
     * Parse a SRID value, anything `<= 0` is unknown
     *
     * @param srid the SRID to parse
     * @return parsed SRID value
     */
    fun parseSRID(input: Int): Int {
        var srid = input
        if (srid < 0) {
            /* TODO: raise a warning ? */
            srid = 0
        }
        return srid
    }
}
