# mysql-async - an asyncm Netty based, MySQL driver written in Scala 2.10

This is the MySQL part of the async driver collection. As the PostgreSQL version, it is not supposed to be a JDBC
replacement, but a simpler solution for those that need something that queries and then returns rows.

You can find more information about the MySQL network protocol [here](http://dev.mysql.com/doc/internals/en/client-server-protocol.html).

## What can it do now?

* connect do databases with the **mysql_native_password** method (that's the usual way)
* execute common statements
* execute prepared statements
* supports MySQL servers from 4.1 and above
* supports most available database types

## Supported types

One important thing to take into account here is that `time` in MySQL is not exactly a time in hours, minutes, seconds.
It's a period/duration and it can be expressed days too (you could, for instance, say that a time is
__-120d 19:27:30.000 001__. As much as this does not make much sense, that is how it was implemented at the database
and as a driver we need to stay true to it, so, while you **can** send `java.sql.Time` and `LocalTime` objects to the
database, when reading these values you will always receive a `scala.concurrent.Duration` object since it is the closest
thing we have to what a `time` value in MySQL means.

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
varcgar | String
time | scala.concurrent.Duration
blob | Array[Byte]

Now when you're using prepared statements:

Scala/Java type | MySQL type
--- | --- | ---
Byte | tinyint
Short | smallint
Int | mediumint
Float | float
Double | double
BigDecimal | numeric
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