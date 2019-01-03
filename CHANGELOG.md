# Changelog

## Next version

* Fix warnings.

## 0.8.59

* Update dependencies:
  * kotlin 1.3.11
  * coroutines 1.1.0
  * kotlin-logging 1.6.22

## 0.8.58

* Update netty to `4.1.32.Final`.
* Fix issue https://github.com/jasync-sql/jasync-sql/issues/63

## 0.8.57

* Fix issue https://github.com/jasync-sql/jasync-sql/issues/51

## 0.8.56

* Warnings cleanup.
* Fix visibility modifiers.
* Issue #59 - add logging to debug it.

## 0.8.55

* Add timeout on netty socket connect: https://github.com/jasync-sql/jasync-sql/pull/52
* Internal changes in Charset mapper: https://github.com/jasync-sql/jasync-sql/pull/55
* Code formatting: https://github.com/jasync-sql/jasync-sql/pull/56

## 0.8.54

* Change result set to be not nullable and have empty default value: https://github.com/jasync-sql/jasync-sql/issues/48

## 0.8.53

* Issue #46 Add `ConnectionPoolConfigurationBuilder`.

## 0.8.52

* Fix a bug in postgres ByteArray encoding.
* Add tests that were not running.
* Change the configurator api (it instead of this).

## 0.8.51

* For the changelist see this PR: https://github.com/jasync-sql/jasync-sql/pull/43
  * Fix a bug in Object Pool for cases connection was broken on return and futures are waiting in queue.
  * Create a new way to configure the pool, with `ConnectionPoolConfiguration` all examples will use a pool because using a connection directly is error prone.
  * Remove old implementation of ConnectionPool and made Actor based the default.
  * Remove deprecated ObjectFactory methods: `createBlocking()` and `testBlocking()`
  * Remove deprecated Configuration properties: `connectTimeout()` and `testTimeout()`
  
## 0.8.50

* Add handling for channel unregister state to close the connection. This will remove the need to have a query timeout for cases channel was disconnected. See: https://github.com/jasync-sql/jasync-sql/pull/40
* Change Actor pool to use SupervisorJob - more consistent in case of cancellation.
* Change internal interface of TimerScheduler.

## 0.8.49

* Change timeout implementation not to use delegate.

## 0.8.48

* Fix bug in case channel become inactive: https://github.com/jasync-sql/jasync-sql/pull/39

## 0.8.47

* Add MySQL connection logging to assist in leaks detection.

## 0.8.46

* Add scope to object pool actor: https://github.com/jasync-sql/jasync-sql/pull/38
* Downgrade to kotlin 1.3.0 as build failed, see issue https://youtrack.jetbrains.com/issue/KT-28223
* Make NextGenConnectionPool implements AsyncObjectPool

## 0.8.45

* Add methods to NextGenConnectionPool to match original ConnectionPool.

## 0.8.44

* Refactor to `Try` And `Future` utils.
* Remove unnecessary `fillInStacktrace()`.
* Add coroutines extensions with `SuspendingConnection`.
* Update kotlin-logging to 1.6.20.

## 0.8.43

* Initial support for ActorBasedObjectPool via `NextGenConnectionPool` (name is not final yet)
* Update to Kotlin `1.3.0`.

## 0.8.41

* Fix a bug that coloum names was empty when executing more than one query: https://github.com/jasync-sql/jasync-sql/pull/29

## 0.8.40

* Fixed sources jar, it was empty in previous releases.
* Since this version (Until further notice) we are releaseing both netty 4.0 and 4.1 artifacts. The normal artifact is netty 4.1. Netty 4.0 version looks like this `0.8.40-netty4.0` etc'. We also reduced the required dependency to `netty-transport` and `netty-handler` instead of `netty-all`. See: https://github.com/jasync-sql/jasync-sql/pull/27.

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
