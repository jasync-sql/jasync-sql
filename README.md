# jasync-sql

The main goal for this project is to implement simple, async, performant and reliable database drivers for
PostgreSQL and MySQL in Kotlin. This is not supposed to be a JDBC replacement, these drivers aim to cover the common
process of _send a statement, get a response_ that you usually see in applications out there. So it's unlikely
there will be support for updating result sets live or stuff like that.


This project is a port of [mauricio/postgresql-async](https://github.com/mauricio/postgresql-async) to Kotlin.
Why? Because the original lib is not maintained anymore, We use it in [ob1k](https://github.com/outbrain/ob1k), and would like to get rid of Scala dependency there.


This project always returns [JodaTime](http://joda-time.sourceforge.net/) when dealing with date types and not the
`java.util.Date` class.

If you want information specific to the drivers, check the [PostgreSQL README](postgresql-async/README.md) and the
[MySQL README](mysql-async/README.md).

You can view the project's [CHANGELOG here](CHANGELOG.md).
