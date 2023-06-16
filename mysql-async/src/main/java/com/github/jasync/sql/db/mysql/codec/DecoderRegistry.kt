package com.github.jasync.sql.db.mysql.codec

import com.github.jasync.sql.db.column.BigDecimalEncoderDecoder
import com.github.jasync.sql.db.column.ColumnDecoder
import com.github.jasync.sql.db.column.DateEncoderDecoder
import com.github.jasync.sql.db.column.DoubleEncoderDecoder
import com.github.jasync.sql.db.column.FloatEncoderDecoder
import com.github.jasync.sql.db.column.IntegerEncoderDecoder
import com.github.jasync.sql.db.column.LocalDateTimeEncoderDecoder
import com.github.jasync.sql.db.column.LongEncoderDecoder
import com.github.jasync.sql.db.column.ShortEncoderDecoder
import com.github.jasync.sql.db.column.StringEncoderDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.BigDecimalDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.BinaryDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.ByteArrayDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.ByteDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.DateDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.DoubleDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.FloatDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.IntegerDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.LongDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.NullDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.ShortDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.StringDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.TimeDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.TimestampDecoder
import com.github.jasync.sql.db.mysql.column.ByteArrayColumnDecoder
import com.github.jasync.sql.db.mysql.column.ColumnTypes
import com.github.jasync.sql.db.mysql.util.CharsetMapper
import com.github.jasync.sql.db.util.XXX
import java.nio.charset.Charset
import com.github.jasync.sql.db.column.ByteDecoder as TextByteDecoder
import com.github.jasync.sql.db.mysql.column.TimeDecoder as TextTimeDecoder

class DecoderRegistry(charset: Charset) {

    private val bigDecimalDecoder = BigDecimalDecoder(charset)
    private val stringDecoder = StringDecoder(charset)

    fun binaryDecoderFor(columnType: Int, charsetCode: Int): BinaryDecoder {
        return when (columnType) {
            ColumnTypes.FIELD_TYPE_VARCHAR,
            ColumnTypes.FIELD_TYPE_JSON,
            ColumnTypes.FIELD_TYPE_ENUM -> this.stringDecoder
            ColumnTypes.FIELD_TYPE_BLOB,
            ColumnTypes.FIELD_TYPE_LONG_BLOB,
            ColumnTypes.FIELD_TYPE_MEDIUM_BLOB,
            ColumnTypes.FIELD_TYPE_TINY_BLOB,
            ColumnTypes.FIELD_TYPE_VAR_STRING,
            ColumnTypes.FIELD_TYPE_STRING -> {
                if (charsetCode == CharsetMapper.Binary) {
                    ByteArrayDecoder
                } else {
                    this.stringDecoder
                }
            }
            ColumnTypes.FIELD_TYPE_BIT -> ByteArrayDecoder
            ColumnTypes.FIELD_TYPE_LONGLONG -> LongDecoder
            ColumnTypes.FIELD_TYPE_LONG, ColumnTypes.FIELD_TYPE_INT24 -> IntegerDecoder
            ColumnTypes.FIELD_TYPE_YEAR, ColumnTypes.FIELD_TYPE_SHORT -> ShortDecoder
            ColumnTypes.FIELD_TYPE_TINY -> ByteDecoder
            ColumnTypes.FIELD_TYPE_DOUBLE -> DoubleDecoder
            ColumnTypes.FIELD_TYPE_FLOAT -> FloatDecoder
            ColumnTypes.FIELD_TYPE_NUMERIC,
            ColumnTypes.FIELD_TYPE_DECIMAL,
            ColumnTypes.FIELD_TYPE_NEW_DECIMAL -> this.bigDecimalDecoder
            ColumnTypes.FIELD_TYPE_DATETIME, ColumnTypes.FIELD_TYPE_TIMESTAMP -> TimestampDecoder
            ColumnTypes.FIELD_TYPE_DATE -> DateDecoder
            ColumnTypes.FIELD_TYPE_TIME -> TimeDecoder
            ColumnTypes.FIELD_TYPE_NULL -> NullDecoder
            else -> XXX("not implemenetd for $columnType")
        }
    }

    fun textDecoderFor(columnType: Int, charsetCode: Int): ColumnDecoder {
        return when (columnType) {
            ColumnTypes.FIELD_TYPE_DATE -> DateEncoderDecoder
            ColumnTypes.FIELD_TYPE_DATETIME,
            ColumnTypes.FIELD_TYPE_TIMESTAMP -> LocalDateTimeEncoderDecoder
            ColumnTypes.FIELD_TYPE_DECIMAL,
            ColumnTypes.FIELD_TYPE_NEW_DECIMAL,
            ColumnTypes.FIELD_TYPE_NUMERIC -> BigDecimalEncoderDecoder
            ColumnTypes.FIELD_TYPE_DOUBLE -> DoubleEncoderDecoder
            ColumnTypes.FIELD_TYPE_FLOAT -> FloatEncoderDecoder
            ColumnTypes.FIELD_TYPE_INT24 -> IntegerEncoderDecoder
            ColumnTypes.FIELD_TYPE_LONG -> IntegerEncoderDecoder
            ColumnTypes.FIELD_TYPE_LONGLONG -> LongEncoderDecoder
            ColumnTypes.FIELD_TYPE_NEWDATE -> DateEncoderDecoder
            ColumnTypes.FIELD_TYPE_SHORT -> ShortEncoderDecoder
            ColumnTypes.FIELD_TYPE_TIME -> TextTimeDecoder
            ColumnTypes.FIELD_TYPE_TINY -> TextByteDecoder
            ColumnTypes.FIELD_TYPE_VARCHAR,
            ColumnTypes.FIELD_TYPE_ENUM -> StringEncoderDecoder
            ColumnTypes.FIELD_TYPE_YEAR -> ShortEncoderDecoder
            ColumnTypes.FIELD_TYPE_BIT -> ByteArrayColumnDecoder
            ColumnTypes.FIELD_TYPE_BLOB,
            ColumnTypes.FIELD_TYPE_VAR_STRING,
            ColumnTypes.FIELD_TYPE_STRING -> {
                if (charsetCode == CharsetMapper.Binary) {
                    ByteArrayColumnDecoder
                } else {
                    StringEncoderDecoder
                }
            }
            else -> StringEncoderDecoder
        }
    }
}
