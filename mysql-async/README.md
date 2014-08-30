<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
**Table of Contents**

- [mysql-async - an async, Netty based, MySQL driver written in Scala 2.10 and 2.11](#mysql-async---an-async-netty-based-mysql-driver-written-in-scala-210)
	- [What can it do now?](#what-can-it-do-now)
	- [Gotchas](#gotchas)
	- [Supported types](#supported-types)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->

# mysql-async - an async, Netty based, MySQL driver written in Scala 2.10 and 2.11

This is the MySQL part of the async driver collection. As the PostgreSQL version, it is not supposed to be a JDBC replacement, but a simpler solution for those that need something that queries and then returns rows.

You can find more information about the MySQL network protocol [here](http://dev.mysql.com/doc/internals/en/client-server-protocol.html).

## What can it do now?

* connect do databases with the **mysql_native_password** method (that's the usual way)
* execute common statements
* execute prepared statements
* supports MySQL servers from 4.1 and above (should also work the same way when using MariaDB or other MySQL derived projects)
* supports most available database types

## Gotchas

* `unsigned` types are not supported, their behaviour when using this driver is undefined.
* Prior to version [5.6.4](http://dev.mysql.com/doc/refman/5.6/en/fractional-seconds.html) MySQL truncates millis in `datetime`, `timestamp` and `time` fields. If your date has millis,
  they will be gone ([docs here](http://dev.mysql.com/doc/refman/5.0/en/fractional-seconds.html))
* If using `5.6` support for microseconds on `timestamp` fields (using the `timestamp(3)` syntax) you can't
  go longer than 3 in precision since `JodaTime` and `Date` objects in Java only go as far as millis and not micro.
  For `time` fields, since `Duration` is used, you get full microsecond precision.
* Timezone support is rather complicated ([see here](http://dev.mysql.com/doc/refman/5.5/en/time-zone-support.html)),
  avoid using timezones in MySQL. This driver just stores the dates as they are and won't perform any computation
  or calculation. I'd recommend using only `datetime` fields and avoid `timestamp` fields as much as possible.
* `time` in MySQL is not exactly a time in hours, minutes, seconds. It's a period/duration and it can be expressed in
  days too (you could, for instance, say that a time is __-120d 19:27:30.000 001__). As much as this does not make much
  sense, that is how it was implemented at the database and as a driver we need to stay true to it, so, while you
  **can** send `java.sql.Time` and `LocalTime` objects to the database, when reading these values you will always
  receive a `scala.concurrent.Duration` object since it is the closest thing we have to what a `time` value in MySQL means.
* MySQL can store dates with values like `0000-00-00` or `0000-00-00 00:00:00` but it's not possible to represent dates   like this in Java (nor there would actually be a date with a zero day or month, this is just MySQL being lenient on    invalid dates) so the driver just returns `null` for any case like that.

## Supported types

When you are receiving data from a `ResultSet`:

MySQL type | Scala/Java type
--- | --- | ---
date | LocalDate
datetime | LocalDateTime
new_date | LocalDate
timestamp | LocalDateTime
tinyint | Byte
smallint | Short
year | Short
float | Float
double | Double
int24 | Int
mediumint | Int
bigint | Long
numeric | BigDecimal
new_decimal | BigDecimal
decimal | BigDecimal
string | String
var_string | String
varchar | String
time | scala.concurrent.Duration
text | String
enum | String
blob | Array[Byte]

Now when you're setting parameters for a prepared statement:

Scala/Java type | MySQL type
--- | --- | ---
Byte | tinyint
Short | smallint
Int | mediumint
Float | float
Double | double
BigDecimal | decimal
LocalDate | date
DateTime | timestamp
scala.concurrent.Duration | time
java.sql.Date | date
java.util.Date | timestamp
java.sql.Timestamp | timestamp
java.sql.Time | time
String | string
Array[Byte] | blob

You don't have to match exact values when sending parameters for your prepared statements, MySQL is usually smart
enough to understand that if you have sent an Int to `smallint` column it has to truncate the 4 bytes into 2.
