# Changelog

## 0.8.37-netty4.0

* A version with netty 4.0 instead of 4.1. Source code is compatible but interface was changed. See: https://github.com/jasync-sql/jasync-sql/pull/27.

## 0.8.36

* Add type safety to RowData:
  * https://github.com/jasync-sql/jasync-sql/wiki/Type-Safety
  * https://github.com/jasync-sql/jasync-sql/pull/24

## 0.8.35

* Fix postgres bug when query has no result.
* Moved samples to https://github.com/jasync-sql/jasync-sql/tree/master/samples
* Created wiki: https://github.com/jasync-sql/jasync-sql/wiki

## 0.8.34

* Fix transactions support - see [#21](https://github.com/jasync-sql/jasync-sql/pull/21).

## 0.8.33

* Make `RowData` interface extends `List<Any?>`.

## 0.8.32

* Add [PartitionedConnectionPool](https://github.com/jasync-sql/jasync-sql/blob/master/db-async-common/src/main/java/com/github/jasync/sql/db/pool/PartitionedConnectionPool.kt).
* Removed execution context from `connection.transaction()` method parameters.


## 0.8.30

* Fix issue [#15](https://github.com/jasync-sql/jasync-sql/issues/15) - ancient performance issue with numbers of original lib.

## 0.8.20

* Initial PostgreSQL + MySQL support.

## Old Changes of Scala lib

See [the older changelog](CHANGELOG-old.md).
