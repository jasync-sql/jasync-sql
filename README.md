# [jasync-sql](https://github.com/jasync-sql/jasync-sql) [![Chat at https://gitter.im/jasync-sql/support](https://badges.gitter.im//jasync-sql/support.svg)](https://gitter.im//jasync-sql/support) [ ![Download](https://api.bintray.com/packages/jasync-sql/jasync-sql/jasync-sql/images/download.svg) ](https://bintray.com/jasync-sql/jasync-sql/jasync-sql/_latestVersion) [![Build Status](https://travis-ci.org/jasync-sql/jasync-sql.svg?branch=master)](https://travis-ci.org/jasync-sql/jasync-sql) [![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](https://github.com/jasync-sql/jasync-sql/blob/master/LICENSE)


The main goal for this project is to implement simple, async, performant and reliable database drivers for
PostgreSQL and MySQL in Kotlin. This is not supposed to be a JDBC replacement, these drivers aim to cover the common
process of _send a statement, get a response_ that you usually see in applications out there. So it's unlikely
there will be support for updating result sets live or stuff like that.

[Show your ❤ with a ★](https://github.com/jasync-sql/jasync-sql/stargazers)

## Getting started

```Java
// Connection to MySQL DB
Connection connection = new MySQLConnection(
      new Configuration(
        "username",
        "host.com",
        3306,
        "password",
        "schema"
      )
    );
// Connection to PostgreSQL DB    
Connection connection = new PostgreSQLConnection(
      new Configuration(
        "username",
        "host.com",
        5324,
        "password",
        "schema"
      )
    );
// And then connect    
CompletableFuture<Connection> connectFuture = connection.connect()
// Wait for connection to be ready   
// ...    
// Execute query
CompletableFuture<QueryResult> future = connection.sendPreparedStatement("select * from table");
// Close the connection
connection.disconnect().get()
```

The above example is a simple usage of the driver. In most real world scenarios there is one missing part here. Each `Connection` from above is capable of handling one query at a time. In order to be able to execute multiple connections simultanously one should use a `ConnectionPool`.  

`ConnectionPool` is responsible to manage connections, borrow them for query execution and validate they are still alive. Aside from construction, `ConnectionPool` has the same interface as a `Connection`. Here is how a connection pool is constructed:

```Java
PoolConfiguration poolConfiguration = new PoolConfiguration(
        100,                            // maxObjects
        TimeUnit.MINUTES.toMillis(15),  // maxIdle
        10_000,                         // maxQueueSize
        TimeUnit.SECONDS.toMillis(30)   // validationInterval
);
ConnectionPool connectionPool = ConnectionPool(
        // for PostgreSQL use PostgreSQLConnectionFactory instead of MySQLConnectionFactory
                                  new MySQLConnectionFactory(configuration), poolConfiguration);
```

See a full example at [jasync-mysql-example](https://github.com/jasync-sql/jasync-mysql-example) and [jasync-postgresql-example](https://github.com/jasync-sql/jasync-postgresql-example).

## Download

### Maven

```xml
<!-- mysql -->
<dependency>
  <groupId>com.github.jasync-sql</groupId>
  <artifactId>jasync-mysql</artifactId>
  <version>0.8.30</version>
</dependency>
<!-- postgresql -->
<dependency>
  <groupId>com.github.jasync-sql</groupId>
  <artifactId>jasync-postgresql</artifactId>
  <version>0.8.30</version>
</dependency>
<!-- add jcenter repo: -->
<repositories>
  <repository>
    <id>jcenter</id>
    <url>https://jcenter.bintray.com/</url>
  </repository>
</repositories>
```

### Gradle

```gradle
dependencies {
  // mysql
  compile 'com.github.jasync-sql:jasync-mysql:0.8.30'
  // postgresql
  compile 'com.github.jasync-sql:jasync-postgresql:0.8.30'
}
// add jcenter repo:
repositories {
    jcenter()
}
```

## Overview

This project is a port of [mauricio/postgresql-async](https://github.com/mauricio/postgresql-async) to Kotlin.  
Why? Because the original lib is not maintained anymore, We use it in [ob1k](https://github.com/outbrain/ob1k), and would like to remove the Scala dependency in ob1k.

This project always returns [JodaTime](http://joda-time.sourceforge.net/) when dealing with date types and not the
`java.util.Date` class. (We plan to move to jdk-8 dates).

If you want information specific to the drivers, check the [PostgreSQL README](postgresql-async/README.md) and the
[MySQL README](mysql-async/README.md).

You can view the project's [CHANGELOG here](CHANGELOG.md).

## Who is using it

* [Outbrain/ob1k-db](https://github.com/outbrain/ob1k/).

## Support

* Open an issue here: https://github.com/jasync-sql/jasync-sql/issues
* Chat on gitter: https://gitter.im/jasync-sql/support

## More links

* [How we started](https://medium.com/@OhadShai/how-i-ported-10k-lines-of-scala-to-kotlin-in-one-week-c645732d3c1).
* https://github.com/mauricio/postgresql-async - The original (deprecated) lib.
* [Async database access with MySQL, Kotlin and jasync-sql](https://medium.com/@OhadShai/async-database-access-with-mysql-kotlin-and-jasync-sql-dbfdb8e7fd04)
* [Issue with NUMERIC](https://medium.com/@OhadShai/sometimes-a-small-bug-fix-can-lead-to-an-avalanche-f6ded2ecf53d)
* [jasync-sql + javalin example](https://medium.com/@OhadShai/reactive-java-all-the-way-to-the-database-with-jasync-sql-and-javalin-c982365d7dd2)


## Contributing

Pull requests are welcome!  
See [CONTRIBUTING](CONTRIBUTING.md).
