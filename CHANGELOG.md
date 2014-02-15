# Changelog

## 0.2.12 - 2014-01-11

* Do not check for handshake requests after a real handshake has happened already - MySQL - #80;

## 0.2.11 - 2014-01-11

* LISTEN/NOTIFY support for PostgreSQL
* Driver logs prepared statement data for PostgreSQL calls when logging is set to debug - #77;
* MySQL and PostgreSQL drivers log network bytes read/written when logging is set to trace;
* PostgreSQL now correctly sends JSON to JSON fields without requiring a cast - #75;
* LocalDateTime is not printed (driver fails silently) - #74;
* Support for ENUM types on PostgreSQL - #75;
* Allow configuring the execution context used by PostgreSQL connections - #72
* Naming executors so we can see the threads created - #71

## 0.2.10 - 2013-12-18

* Removed application_name from PostgreSQL default connection values - #70

## 0.2.9 - 2013-12-01

* PostgreSQL driver cannot parse value set by current_timestamp with timezone - #51
* Add AsyncObjectPool.use to combine take and giveBack - #53
* Add support for postgres interval type as Period - #56
* Connection mutex improvements for issue - #59
* Improve URL parser to allow missing hostname/dbname - #64
* Decode OIDs as Long - #62
* Improve placeholders and prepared statement handling - #65
* ResultSet.columnNames order does not match ResultSet order - #61
* Add Connection.inTransaction to wrap queries in a transaction block - #54
* Add support for MySQL BINARY/VARBINARY types - #55

## 0.2.8 - 2013-09-24

* Validate the number of parameters for prepared statements - fixes #47
* Adding support for MySQL BIT type - fixes #48
* Use the rows count as the affected rows for MySQL also - fixes #46

## 0.2.7 - 2013-09-09

* Upgrading Netty to 4.0.9
* Removing direct dependency on `logback` and making it depend on SFL4J only, upgrading JodaTime - by @kxbmap
* MySQL doesn't set columnNames in QueryResult - #42
* Timestamps with microseconds fail to be parsed on PostgreSQL - #41

## 0.2.5

* Allow the ClientSocketChannelFactory and ExecutionContext to be given at the connections instead of
 always using the driver provided ones - #38
* Upgraded to Netty 4 - @normanmaurer

## 0.2.4 - 2013-07-06

* Mysql driver fails for null TIMESTAMP
* Mysql driver fails for big strings (TEXT)
* Support (auto convert) Option in prepared stmt parameters - @magro - #30
* Allow database connections strings without port, username and password - @magro - #32
* Support 'postgres' protocol in heroku like db urls - @magro - #33
* Fix jdbc:postgresql url format to use 'user' instead 'username' - @magro - #35


## 0.2.3 - 2013-05-21

* Upgraded Netty to 3.6.6.Final
* Added support for `timestamp(n)` values on MySQL
* Updated docs to reference MySQL's 5.6 support for microseconds - @theon
* MySQL driver returns DateTime instead of LocalDateTime when in text protocol - #24

## 0.2.2 - 2013-05-18

* Implement MySQL support, should be able to execute common statements, prepared statements and login with password (#9)
* Concurrency problem for multiple queries - @fwbrasil - #18
* Support prepared statement with more than 64 characters on PostgreSQL - @fwbrasil - #16
* Do not accept returned connections to pool that aren't ready for query - @fwbrasil - #15
* Multiple executions of a prepared statement that doesn't return rows fail - @fwbrasil - #13
* Optimize match/cases to `@switch` (#10)
* Reimplement the PostgreSQLMD5Digest.java in Scala - (#8)

## 0.1.1 - 2013-04-30

* Query promises fulfilled before cleaning up the query promise cause the futures to either hang forever or fail with a
  "query already running" message (#2)
* Optimize MessageEncoder to use a match instead of a map (#3)
* MessageDecoder should validate sizes and correctly handle negative or too large messages (#4)
* Move generic pool classes to the com.github.mauricio.async.db package (#9)

## 0.1.0 - 2013-04-29

* First public release
