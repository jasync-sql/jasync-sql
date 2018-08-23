/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package com.github.mauricio.async.db.postgresql.column

object ColumnTypes {
  final val Untyped = 0
  final val Bigserial = 20
  final val BigserialArray = 1016
  final val Char = 18
  final val CharArray = 1002
  final val Smallint = 21
  final val SmallintArray = 1005
  final val Integer = 23
  final val IntegerArray = 1007
  final val Numeric = 1700
  // Decimal is the same as Numeric on PostgreSQL
  final val NumericArray = 1231
  final val Real = 700
  final val RealArray = 1021
  final val Double = 701
  final val DoubleArray = 1022
  final val Serial = 23
  final val Bpchar = 1042
  final val BpcharArray = 1014
  final val Varchar = 1043
  // Char is the same as Varchar on PostgreSQL
  final val VarcharArray = 1015
  final val Text = 25
  final val TextArray = 1009
  final val Timestamp = 1114
  final val TimestampArray = 1115
  final val TimestampWithTimezone = 1184
  final val TimestampWithTimezoneArray = 1185
  final val Date = 1082
  final val DateArray = 1182
  final val Time = 1083
  final val TimeArray = 1183
  final val TimeWithTimezone = 1266
  final val TimeWithTimezoneArray = 1270
  final val Interval = 1186
  final val IntervalArray = 1187
  final val Boolean = 16
  final val BooleanArray = 1000
  final val OID = 26
  final val OIDArray = 1028

  final val ByteA = 17
  final val ByteA_Array = 1001

  final val MoneyArray = 791
  final val NameArray = 1003
  final val UUID = 2950
  final val UUIDArray = 2951
  final val XMLArray = 143

  final val Inet = 869
  final val InetArray = 1041
}

/*

    public static final int UNSPECIFIED = 0;
    public static final int INT2 = 21;
    public static final int INT2_ARRAY = 1005;
    public static final int INT4 = 23;
    public static final int INT4_ARRAY = 1007;
    public static final int INT8 = 20;
    public static final int INT8_ARRAY = 1016;
    public static final int TEXT = 25;
    public static final int TEXT_ARRAY = 1009;
    public static final int NUMERIC = 1700;
    public static final int NUMERIC_ARRAY = 1231;
    public static final int FLOAT4 = 700;
    public static final int FLOAT4_ARRAY = 1021;
    public static final int FLOAT8 = 701;
    public static final int FLOAT8_ARRAY = 1022;
    public static final int BOOL = 16;
    public static final int BOOL_ARRAY = 1000;
    public static final int DATE = 1082;
    public static final int DATE_ARRAY = 1182;
    public static final int TIME = 1083;
    public static final int TIME_ARRAY = 1183;
    public static final int TIMETZ = 1266;
    public static final int TIMETZ_ARRAY = 1270;
    public static final int TIMESTAMP = 1114;
    public static final int TIMESTAMP_ARRAY = 1115;
    public static final int TIMESTAMPTZ = 1184;
    public static final int TIMESTAMPTZ_ARRAY = 1185;
    public static final int BYTEA = 17;
    public static final int BYTEA_ARRAY = 1001;
    public static final int VARCHAR = 1043;
    public static final int VARCHAR_ARRAY = 1015;
    public static final int OID = 26;
    public static final int OID_ARRAY = 1028;
    public static final int BPCHAR = 1042;
    public static final int BPCHAR_ARRAY = 1014;
    public static final int MONEY = 790;
    public static final int MONEY_ARRAY = 791;
    public static final int NAME = 19;
    public static final int NAME_ARRAY = 1003;
    public static final int BIT = 1560;
    public static final int BIT_ARRAY = 1561;
    public static final int VOID = 2278;
    public static final int INTERVAL = 1186;
    public static final int INTERVAL_ARRAY = 1187;
    public static final int CHAR = 18; // This is not char(N), this is "char" a single byte type.
    public static final int CHAR_ARRAY = 1002;
    public static final int VARBIT = 1562;
    public static final int VARBIT_ARRAY = 1563;
    public static final int UUID = 2950;
    public static final int UUID_ARRAY = 2951;
    public static final int XML = 142;
    public static final int XML_ARRAY = 143;

*/
