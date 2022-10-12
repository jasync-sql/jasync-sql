package com.github.jasync.sql.db.postgresql.column

@Suppress("unused")
object ColumnTypes {
    const val Untyped = 0
    const val Bigserial = 20
    const val BigserialArray = 1016
    const val Char = 18
    const val CharArray = 1002
    const val Smallint = 21
    const val SmallintArray = 1005
    const val Integer = 23
    const val IntegerArray = 1007
    const val Numeric = 1700
    // Decimal is the same as Numeric on PostgreSQL
    const val NumericArray = 1231
    const val Real = 700
    const val RealArray = 1021
    const val Double = 701
    const val DoubleArray = 1022
    const val Serial = 23
    const val Bpchar = 1042
    const val BpcharArray = 1014
    const val Varchar = 1043
    // Char is the same as Varchar on PostgreSQL
    const val VarcharArray = 1015
    const val Text = 25
    const val TextArray = 1009
    const val Timestamp = 1114
    const val TimestampArray = 1115
    const val TimestampWithTimezone = 1184
    const val TimestampWithTimezoneArray = 1185
    const val Date = 1082
    const val DateArray = 1182
    const val Time = 1083
    const val TimeArray = 1183
    const val TimeWithTimezone = 1266
    const val TimeWithTimezoneArray = 1270
    const val Interval = 1186
    const val IntervalArray = 1187
    const val Boolean = 16
    const val BooleanArray = 1000
    const val OID = 26
    const val OIDArray = 1028

    const val ByteA = 17
    const val ByteA_Array = 1001

    const val MoneyArray = 791
    const val NameArray = 1003
    const val UUID = 2950
    const val UUIDArray = 2951
    const val XMLArray = 143

    const val Inet = 869
    const val InetArray = 1041
    const val Geometry = 18011
}

/*

    public static int UNSPECIFIED = 0;
    public static int INT2 = 21;
    public static int INT2_ARRAY = 1005;
    public static int INT4 = 23;
    public static int INT4_ARRAY = 1007;
    public static int INT8 = 20;
    public static int INT8_ARRAY = 1016;
    public static int TEXT = 25;
    public static int TEXT_ARRAY = 1009;
    public static int NUMERIC = 1700;
    public static int NUMERIC_ARRAY = 1231;
    public static int FLOAT4 = 700;
    public static int FLOAT4_ARRAY = 1021;
    public static int FLOAT8 = 701;
    public static int FLOAT8_ARRAY = 1022;
    public static int BOOL = 16;
    public static int BOOL_ARRAY = 1000;
    public static int DATE = 1082;
    public static int DATE_ARRAY = 1182;
    public static int TIME = 1083;
    public static int TIME_ARRAY = 1183;
    public static int TIMETZ = 1266;
    public static int TIMETZ_ARRAY = 1270;
    public static int TIMESTAMP = 1114;
    public static int TIMESTAMP_ARRAY = 1115;
    public static int TIMESTAMPTZ = 1184;
    public static int TIMESTAMPTZ_ARRAY = 1185;
    public static int BYTEA = 17;
    public static int BYTEA_ARRAY = 1001;
    public static int VARCHAR = 1043;
    public static int VARCHAR_ARRAY = 1015;
    public static int OID = 26;
    public static int OID_ARRAY = 1028;
    public static int BPCHAR = 1042;
    public static int BPCHAR_ARRAY = 1014;
    public static int MONEY = 790;
    public static int MONEY_ARRAY = 791;
    public static int NAME = 19;
    public static int NAME_ARRAY = 1003;
    public static int BIT = 1560;
    public static int BIT_ARRAY = 1561;
    public static int VOID = 2278;
    public static int INTERVAL = 1186;
    public static int INTERVAL_ARRAY = 1187;
    public static int CHAR = 18; // This is not char(N), this is "char" a single byte type.
    public static int CHAR_ARRAY = 1002;
    public static int VARBIT = 1562;
    public static int VARBIT_ARRAY = 1563;
    public static int UUID = 2950;
    public static int UUID_ARRAY = 2951;
    public static int XML = 142;
    public static int XML_ARRAY = 143;

*/
