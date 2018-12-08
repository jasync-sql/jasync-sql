
# postgresql-async - an async Netty based PostgreSQL driver.

The main goal of this project is to implement a performant and fully functional async PostgreSQL driver. This project
has no interest in JDBC, it's supposed to be a clean room implementation for people interested in talking directly
to PostgreSQL.

[PostgreSQL protocol information and definition can be found here](http://www.postgresql.org/docs/devel/static/protocol.html)

This driver contains Java code from the [JDBC PostgreSQL](http://jdbc.postgresql.org/) driver under the
`com.github.jasync.sql.db.postgresql.util` package consisting of the `ParseURL` class.

## What can it do now?

- connect to a database with or without authentication (supports MD5 and cleartext authentication methods)
- receive database parameters
- receive database notices
- execute direct queries (without portals/prepared statements)
- portals/prepared statements
- parses most of the basic PostgreSQL types, other types are parsed as string
- date, time and timestamp types are handled as JodaTime objects and **not** as **java.util.Date** objects
- all work is done using the new `scala.concurrent.Future` and `scala.concurrent.Promise` objects
- support for Byte arrays if using PostgreSQL >= 9.0
- support for LISTEN/NOTIFY operations (check [ListenNotifySpec](https://github.com/mauricio/postgresql-async/blob/master/postgresql-async/src/test/scala/com/github/mauricio/async/db/postgresql/ListenNotifySpec.scala) for an example on how to use it );

## What is missing?

- more authentication mechanisms
- benchmarks
- more tests (run the `jacoco:cover` sbt task and see where you can improve)
- timeout handler for initial handshare and queries
- implement byte array support for PostgreSQL <= 8

## Supported Java types and their destination types on PostgreSQL

Moved to https://github.com/jasync-sql/jasync-sql/wiki/PostgreSQL-Types
