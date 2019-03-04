package com.github.jasync.r2dbc.mysql

import com.github.jasync.sql.db.RowData
import io.r2dbc.spi.Row


class JasyncRow(private val rowData: RowData) : Row {

//    private val TYPED_BY_NAME_ACCESSORS = HashMap<Class<*>, BiFunction<RowData, String, *>>()
//    private val TYPED_BY_INDEX_ACCESSORS = HashMap<Class<*>, BiFunction<RowData, Int, *>>()
//
//    init
//    {
//        registerAccessor(Short::class.java, Row ::getShort, Row::getShort);
//        registerAccessor(Boolean::class.java, Row ::getBoolean, Row::getBoolean);
//        registerAccessor(Integer::class.java, Row ::getInteger, Row::getInteger);
//        registerAccessor(Long::class.java, Row ::getLong, Row::getLong);
//        registerAccessor(Float::class.java, Row ::getFloat, Row::getFloat);
//        registerAccessor(Double::class.java, Row ::getDouble, Row::getDouble);
//        registerAccessor(String::class.java, Row ::getString, Row::getString);
//        registerAccessor(Json::class.java, Row ::getJson, Row::getJson);
//        registerAccessor(Buffer::class.java, Row ::getBuffer, Row::getBuffer);
//        registerAccessor(Temporal::class.java, Row ::getTemporal, Row::getTemporal);
//        registerAccessor(LocalDate::class.java, Row ::getLocalDate, Row::getLocalDate);
//        registerAccessor(LocalTime::class.java, Row ::getLocalTime, Row::getLocalTime);
//        registerAccessor(LocalDateTime::class.java, Row ::getLocalDateTime, Row::getLocalDateTime);
//        registerAccessor(OffsetTime::class.java, Row ::getOffsetTime, Row::getOffsetTime);
//        registerAccessor(OffsetDateTime::class.java, Row ::getOffsetDateTime, Row::getOffsetDateTime);
//        registerAccessor(UUID::class.java, Row ::getUUID, Row::getUUID);
//        registerAccessor(BigDecimal::class.java, Row ::getBigDecimal, Row::getBigDecimal);
//        registerAccessor(Numeric::class.java, Row ::getNumeric, Row::getNumeric);
//        registerAccessor(Point::class.java, Row ::getPoint, Row::getPoint);
//        registerAccessor(Line::class.java, Row ::getLine, Row::getLine);
//        registerAccessor(LineSegment::class.java, Row ::getLineSegment, Row::getLineSegment);
//        registerAccessor(Box::class.java, Row ::getBox, Row::getBox);
//        registerAccessor(Path::class.java, Row ::getPath, Row::getPath);
//        registerAccessor(Polygon::class.java, Row ::getPolygon, Row::getPolygon);
//        registerAccessor(Circle::class.java, Row ::getCircle, Row::getCircle);
//        registerAccessor(Interval::class.java, Row ::getInterval, Row::getInterval);
//    }
//
//    private fun <T> registerAccessor(
//        type: Class<T>,
//        byName: BiFunction<Row, String, T>,
//        byIndex: BiFunction<Row, Int, T>
//    ) {
//
//        TYPED_BY_NAME_ACCESSORS[type] = byName
//        TYPED_BY_INDEX_ACCESSORS[type] = byIndex
//    }


    override fun <T> get(identifier: Any, requestedType: Class<T>): T? {


        if (identifier is String) {

//            val accessor = TYPED_BY_NAME_ACCESSORS[requestedType]
//                ?: throw IllegalArgumentException("Type $requestedType not supported")
//
//            return requestedType.cast(accessor.apply(rowData, identifier))
            return rowData.getAs(identifier)
        }

        if (identifier is Int) {

//            val accessor = TYPED_BY_INDEX_ACCESSORS[requestedType]
//                ?: throw IllegalArgumentException("Type $requestedType not supported")
//
//            return requestedType.cast(accessor.apply(rowData, identifier))
            return rowData.getAs(identifier)
        }

        throw IllegalArgumentException("Identifier must be a String or an Integer")
    }

    override fun get(identifier: Any): Any? {

        if (identifier is String) {
            return rowData.get(identifier)
        }

        if (identifier is Int) {
            return rowData.get(identifier)
        }

        throw IllegalArgumentException("Identifier must be a String or an Integer")
    }
}