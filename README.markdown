# postgresql-netty - an async Netty/NIO based PostgreSQL driver written in Scala

The main goal of this project is to implement a performant and fully functional async PostgreSQL driver. This project
has no interest in JDBC, it's supposed to be a clean room implementation for people interested in talking directly
to PostgreSQL.

[PostgreSQL protocol information and definition can be found here](http://www.postgresql.org/docs/devel/static/protocol.html)

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

## How can I help?

- checkout the source code
- find bugs, find places where performance can be improved
- check the **What is missing** piece
- send a pull request with specs

## Example usage

You can find a small Play 2 app using it [here](https://github.com/mauricio/postgresql-async-app) and a blog post about
it [here](http://mauricio.github.io/2013/04/29/async-database-access-with-postgresql-play-scala-and-heroku.html).

In short, what you would usually do is:
```scala
import com.github.mauricio.async.db.postgresql.DatabaseConnectionHandler
import com.github.mauricio.async.db.util.ExecutorServiceUtils.CachedExecutionContext
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