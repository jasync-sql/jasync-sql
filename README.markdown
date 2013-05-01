# postgresql-async - an async Netty based PostgreSQL driver written in Scala 2.10

The main goal of this project is to implement a performant and fully functional async PostgreSQL driver. This project
has no interest in JDBC, it's supposed to be a clean room implementation for people interested in talking directly
to PostgreSQL.

[PostgreSQL protocol information and definition can be found here](http://www.postgresql.org/docs/devel/static/protocol.html)

Include at your SBT project with:

```scala
"com.github.mauricio" %% "postgresql-async" % "0.1.1"
```

Or Maven:

```xml
<dependency>
  <groupId>com.github.mauricio</groupId>
  <artifactId>postgresql-async_2.10</artifactId>
  <version>0.1.1</version>
</dependency>
```

This driver contains Java code from the [JDBC PostgreSQL](http://jdbc.postgresql.org/) driver under the
`com.github.mauricio.async.db.postgresql.util` package consisting of `PostgreSQLMD5Digest` and `ParseURL` classes.

## What can it do now?

- connect to a database with or without authentication (supports MD5 and cleartext authentication methods)
- receive database parameters
- receive database notices
- execute direct queries (without portals/prepared statements)
- portals/prepared statements
- parses most of the basic PostgreSQL types, other types are parsed as string
- date, time and timestamp types are handled as JodaTime objects and **not** as **java.util.Date** objects
- all work is done using the new `scala.concurrent.Future` and `scala.concurrent.Promise` objects

## What is missing?

- more authentication mechanisms
- benchmarks
- more tests (run the `jacoco:cover` sbt task and see where you can improve)
- timeout handler for initial handshare and queries
- implement byte array support

## What are the design goals?

- fast, fast and faster
- small memory footprint
- avoid copying data as much as possible (we're always trying to use wrap and slice on buffers)
- easy to use, call a method, get a future or a callback and be happy
- never, ever, block
- all features covered by tests
- next to zero dependencies (it currently depends on Netty and SFL4J only)

## How can you help?

- checkout the source code
- find bugs, find places where performance can be improved
- check the **What is missing** piece
- send a pull request with specs

## Main public interface

### Connection

Represents a connection to the database. This is the **root** object you will be using in your application. You will
find two classes that implement this trait, `DatabaseConnectionHandler` and `ConnectionPool`. The different between
them is that `DatabaseConnectionHandler` is a single connection and `ConnectionPool` represents a pool of connections.

To create both you will need a `Configuration` object with your database details. You can create one manually or
create one from a JDBC or Heroku database URL using the `URLParser` object.

### QueryResult

It's the output of running a statement against the database (either using `sendQuery` or `sendPreparedStatement`).
This object will contain the amount of rows, status message and the possible `ResultSet` (Option\[ResultSet]) if the
query returns any rows.

### ResultSet

It's an IndexedSeq\[Array\[Any]], every item is a row returned by the database. The database types are returned as Scala
objects that fit the original type, so `smallint` becomes a `Short`, `numeric` becomes `BigDecimal`, `varchar` becomes
`String` and so on. You can find the whole default transformation list at the `DefaultColumnDecoderRegistry` class.

### Prepared statements

Databases support prepared or precompiled statements. These statements allow the database to precompile the query
on the first execution and reuse this compiled representation on future executions, this makes it faster and also allows
for safer data escaping when dealing with user provided data.

To execute a prepared statement you grab a connection and:

```scala
val connection : Connection = ...
val future = connection.sendPreparedStatement( "SELECT * FROM products WHERE products.name = ?", Array( "Dominion" ) )
```

The `?` (question mark) in the query is a parameter placeholder, it allows you to set a value in that place in the
query without having to escape stuff yourself. The driver itself will make sure this parameter is delivered to the
database in a safe way so you don't have to worry about SQL injection attacks.

The basic numbers, Joda Time date, time, timestamp objects, strings and arrays of these objects are all valid values
as prepared statement parameters and they will be encoded to their respective PostgreSQL types.

Remember that parameters are positional the order they show up at query should be the same as the one in the array or
sequence given to the method call.

## Example usage

You can find a small Play 2 app using it [here](https://github.com/mauricio/postgresql-async-app) and a blog post about
it [here](http://mauricio.github.io/2013/04/29/async-database-access-with-postgresql-play-scala-and-heroku.html).

In short, what you would usually do is:
```scala
import com.github.mauricio.async.db.postgresql.DatabaseConnectionHandler
import com.github.mauricio.async.db.util.ExecutorServiceUtils.FixedExecutionContext
import com.github.mauricio.async.db.util.URLParser
import com.github.mauricio.async.db.{RowData, QueryResult, Connection}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

object BasicExample {

  def main(args: Array[String]) {

    val configuration = URLParser.parse("jdbc:postgresql://localhost:5233/my_database?username=postgres&password=somepassword")
    val connection: Connection = new DatabaseConnectionHandler(configuration)

    Await.result(connection.connect, 5 seconds)

    val future: Future[QueryResult] = connection.sendQuery("SELECT 0")

    val mapResult: Future[Any] = future.map(queryResult => queryResult.rows match {
      case Some(resultSet) => {
        val row : RowData = resultSet.head
        row(0)
      }
      case None => -1
    }
    )

    val result = Await.result( mapResult, 5 seconds )

    println(result)

    connection.disconnect

  }

}
```

First, create a `DatabaseConnectionHandler`, connect it to the database, execute queries (this object is not thread
safe, so you must execute only one query at a time) and work with the futures it returns. Once you are done, call
disconnect and the connection is closed.

You can also use the `ConnectionPool` provided by the driver to simplify working with database connections in your app.
Check the blog post above for more details and the project's ScalaDocs.

## Licence

This project is freely available under the Apache 2 licence, use it at your own risk.