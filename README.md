<img width="550" alt="jasync-sql" src="jas.png" style="max-width:100%;"> 

[![Chat at https://gitter.im/jasync-sql/support](https://badges.gitter.im//jasync-sql/support.svg)](https://gitter.im//jasync-sql/support) [ ![Download](https://api.bintray.com/packages/jasync-sql/jasync-sql/jasync-sql/images/download.svg) ](https://bintray.com/jasync-sql/jasync-sql/jasync-sql/_latestVersion) [![Build Status](https://travis-ci.org/jasync-sql/jasync-sql.svg?branch=master)](https://travis-ci.org/jasync-sql/jasync-sql) [![Apache License V.2](https://img.shields.io/badge/license-Apache%20V.2-blue.svg)](https://github.com/jasync-sql/jasync-sql/blob/master/LICENSE) [![codecov](https://codecov.io/gh/jasync-sql/jasync-sql/branch/master/graph/badge.svg)](https://codecov.io/gh/jasync-sql/jasync-sql) [![Awesome Kotlin Badge](https://kotlin.link/awesome-kotlin.svg)](https://github.com/KotlinBy/awesome-kotlin#libraries-frameworks-database)


[jasync-sql](https://github.com/jasync-sql/jasync-sql) is a Simple, Netty based, asynchronous, performant and reliable database drivers for
PostgreSQL and MySQL written in Kotlin.

[Show your ❤ with a ★](https://github.com/jasync-sql/jasync-sql/stargazers)

## Getting started

```Java
// Connection to MySQL DB
Connection connection = new MySQLConnectionBuilder.createConnectionPool(
               "jdbc:mysql://$host:$port/$database?user=$username&password=$password");
     
// Connection to PostgreSQL DB    
Connection connection = PostgreSQLConnectionBuilder.createConnectionPool(
               "jdbc:postgresql://$host:$port/$database?user=$username&password=$password");

// Execute query
CompletableFuture<QueryResult> future = connection.sendPreparedStatement("select * from table");
// work with result ...
// Close the connection pool
connection.disconnect().get()
```

See a full example at [jasync-mysql-example](https://github.com/jasync-sql/jasync-mysql-example) and [jasync-postgresql-example](https://github.com/jasync-sql/jasync-postgresql-example).  
More samples on the [samples dir](https://github.com/jasync-sql/jasync-sql/tree/master/samples).  

For docs and info see the [wiki](https://github.com/jasync-sql/jasync-sql/wiki).

## Download


### Maven

```xml
<!-- mysql -->
<dependency>
  <groupId>com.github.jasync-sql</groupId>
  <artifactId>jasync-mysql</artifactId>
  <version>0.9.39</version>
</dependency>
<!-- postgresql -->
<dependency>
  <groupId>com.github.jasync-sql</groupId>
  <artifactId>jasync-postgresql</artifactId>
  <version>0.9.39</version>
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
  compile 'com.github.jasync-sql:jasync-mysql:0.9.39'
  // postgresql
  compile 'com.github.jasync-sql:jasync-postgresql:0.9.39'
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

You can view the project's change log [here](CHANGELOG.md).

**Follow us on twitter: [@jasyncs](https://twitter.com/Jasyncs).**

## Who is using it

* [Outbrain/ob1k-db](https://github.com/outbrain/ob1k/).
* https://github.com/humb1t/jpom
* [Zeko Data Mapper](https://github.com/darkredz/Zeko-Data-Mapper)
* [Vert.x](https://github.com/vert-x3/vertx-mysql-postgresql-client)
* [vertx-jooq async module](https://github.com/jklingsporn/vertx-jooq)
* [micronaut](https://github.com/micronaut-projects/micronaut-sql)
* [spring-r2dbc](https://www.youtube.com/watch?v=DvO4zLVDkMs)

Add your name here!

Is it used in production on large scale?  

<img width="550" alt="jasync-sql-production" src="production.png" style="max-width:100%;"> 

The graph above is from only couple of services using it. Y-Axis is # of queries per minute.

There is also a [TechEmpower test](https://github.com/TechEmpower/FrameworkBenchmarks/pull/4247) using `ktor` and `jasync-sql`.

## Support

* Open an issue here: https://github.com/jasync-sql/jasync-sql/issues
* Chat on gitter: https://gitter.im/jasync-sql/support
* Ask a question in StackOverflow with [jasync-sql](https://stackoverflow.com/questions/tagged/jasync-sql) tag.

## More links

* [How we cloned the original lib](https://hackernoon.com/how-i-ported-10k-lines-of-scala-to-kotlin-in-one-week-c645732d3c1).
* https://github.com/mauricio/postgresql-async - The original (deprecated) lib.
* [Async database access with MySQL, Kotlin and jasync-sql](https://medium.com/@OhadShai/async-database-access-with-mysql-kotlin-and-jasync-sql-dbfdb8e7fd04).
* [Issue with NUMERIC](https://medium.com/@OhadShai/sometimes-a-small-bug-fix-can-lead-to-an-avalanche-f6ded2ecf53d).
* [jasync-sql + javalin example](https://medium.com/@OhadShai/reactive-java-all-the-way-to-the-database-with-jasync-sql-and-javalin-c982365d7dd2).
* [jasync-sql + ktor + coroutines example](https://medium.com/@OhadShai/async-with-style-kotlin-web-backend-with-ktor-coroutines-and-jasync-mysql-b34e8c83e4bd).
* [jasync-sql + spring + kotlin example](https://github.com/godpan/spring-kotlin-jasync-sql).
* [How to implement an Object Pool with an Actor in Kotlin](https://medium.freecodecamp.org/how-to-implement-an-object-pool-with-an-actor-in-kotlin-ed06d3ba6257).
* [Coroutines usage](https://medium.com/@OhadShai/just-a-small-example-of-how-kotlin-coroutines-are-great-c9774fe8434).


## Contributing

Pull requests are welcome!  
See [CONTRIBUTING](CONTRIBUTING.md).
