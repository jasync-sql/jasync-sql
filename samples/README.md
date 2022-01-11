# Samples for [jasync-sql](https://github.com/jasync-sql/jasync-sql)

A collection of ready-to-use samples for [jasync-sql](https://github.com/jasync-sql/jasync-sql).

Sample | Labels | Blog post
--- | --- | ---
[spring](https://github.com/jasync-sql/jasync-sql/tree/master/samples/spring-kotlin) | `spring` `kotlin`
[javalin](https://github.com/jasync-sql/jasync-sql/tree/master/samples/postgres-javalin) | `javalin` `postgres` | [Async Java all the way to the database with jasync-sql and javalin](https://medium.com/@OhadShai/reactive-java-all-the-way-to-the-database-with-jasync-sql-and-javalin-c982365d7dd2)
[ktor](https://github.com/jasync-sql/jasync-sql/tree/master/samples/ktor) | `ktor` `kotlin` `coroutines` `mysql` | [Async with style — Kotlin web backend with ktor, coroutines, and jasync-mysql](https://medium.com/@OhadShai/async-with-style-kotlin-web-backend-with-ktor-coroutines-and-jasync-mysql-b34e8c83e4bd)
[vertx](https://github.com/jasync-sql/jasync-sql/tree/master/samples/jasync-vertx-examples) | `vertx` `mysql` `vertx-mysql-postgresql-client` | [Async DB access with vertx-mysql-postgresql-client](https://medium.com/outbrain-engineering/async-db-access-with-vertx-mysql-postgresql-client-e5e509745598)
[r2dbc](https://github.com/jasync-sql/jasync-sql/tree/master/samples/mysql-r2dbc) | `r2dbc` `mysql` `spring` `web-flux` | More detials on the [video](https://www.youtube.com/watch?v=DvO4zLVDkMs) and [repo](https://github.com/joshlong/reactive-mysql-with-jasync-and-r2dbc). There is also a blog post [here](https://medium.com/w-logs/jdbc-for-spring-webflux-spring-data-r2dbc-99690208cfeb) with [this](https://github.com/vxavictor513/kubenote/tree/master/r2dbc-and-flyway) repo.


## Cut-and-pasting samples

Each sample is a standalone Gradle project that can be cut-and-pasted to get started with your own project.   
Cut-and-paste the directory of the corresponding sample together with 
its build scripts.

## Other samples

* [MySQL](https://github.com/jasync-sql/jasync-mysql-example)
* [PostgreSQL](https://github.com/jasync-sql/jasync-postgresql-example)

## Run dockers
* Mysql: docker-compose -f dockers/mysql/docker-compose.yml up



