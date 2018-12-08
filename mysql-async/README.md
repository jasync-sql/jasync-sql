
# mysql-async - an async, Netty based, MySQL driver.

This is the MySQL part of the async driver collection. As the PostgreSQL version, it is not supposed to be a JDBC replacement, but a simpler solution for those that need something that queries and then returns rows.

You can find more information about the MySQL network protocol [here](http://dev.mysql.com/doc/internals/en/client-server-protocol.html).

## What can it do now?

* connect do databases with the **mysql_native_password** method (that's the usual way)
* execute common statements
* execute prepared statements
* supports MySQL servers from 4.1 and above (should also work the same way when using MariaDB or other MySQL derived projects)
* supports most available database types

## Supported types

Moved to https://github.com/jasync-sql/jasync-sql/wiki/MySQL-Types
