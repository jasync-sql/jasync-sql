# Changelog

## 2.0.3

* Move to github actions for ci/cd [#269](https://github.com/jasync-sql/jasync-sql/pull/269).
* postgresql-async test suite doesn't have connection configurability [#264](https://github.com/jasync-sql/jasync-sql/pull/264).

## 2.0.0

* Switch to Java Date/Time API from Joda Time [#233](https://github.com/jasync-sql/jasync-sql/pull/233).
* For postgresql, add support for threeten.PeriodDuration for interval data type. [#233](https://github.com/jasync-sql/jasync-sql/pull/233).

## 1.2.3

* Enable CLIENT_FOUND_ROWS in r2dbc-mysql in favor of spring-data-r2dbc [#240](https://github.com/jasync-sql/jasync-sql/pull/240).
* Fix SSL Connection to MySQL [#237](https://github.com/jasync-sql/jasync-sql/pull/237).
* Add explicit dependency on kotlin-reflect.

## 1.2.1

* Deploy directly to maven central [#232](https://github.com/jasync-sql/jasync-sql/pull/232).

## 1.1.7

* Support SSL for MySQL [#222](https://github.com/jasync-sql/jasync-sql/pull/222).
* Query timeout is not respected when host is not listening [#223](https://github.com/jasync-sql/jasync-sql/pull/223).
* PostgreSQLConnectionFactory logs eintire db configuration including password [#226](https://github.com/jasync-sql/jasync-sql/pull/226).

## 1.1.6

* Add scram-sha-256 authentication (via SASL) to the PostgreSQL client [#219](https://github.com/jasync-sql/jasync-sql/pull/219).
* Upgrade to Kotlin 1.4.20 [#216](https://github.com/jasync-sql/jasync-sql/pull/216).

## 1.1.5

* Add client SSL configuration [#214](https://github.com/jasync-sql/jasync-sql/pull/214).

## 1.1.3

* Convert Gradle to Kotlin script: https://github.com/jasync-sql/jasync-sql/pull/201.
* Apply ktlint on build and fix errors: https://github.com/jasync-sql/jasync-sql/pull/204.
* Prevent false leak detection in tests: https://github.com/jasync-sql/jasync-sql/pull/203.

## 1.1.1,1.1.2

* Test versions for https://github.com/jasync-sql/jasync-sql/pull/201.

## 1.1.0

* Add and artifact for the object pool: https://github.com/jasync-sql/jasync-sql/issues/198.

## 1.0.19

* Same as `1.0.18`.

## 1.0.18

* Remove sensitive info from log: https://github.com/jasync-sql/jasync-sql/pull/199.

## 1.0.17

* Add r2dbc query timeout - https://github.com/jasync-sql/jasync-sql/issues/193.
* Allow to bypass query interceptors for test queries - https://github.com/jasync-sql/jasync-sql/issues/188.

## 1.0.16

Corrupted version.

## 1.0.15

* Update netty to `4.1.49.Final` - https://github.com/jasync-sql/jasync-sql/pull/191.

## 1.0.14

* Fix collisions between Prepared Statements in postgres - https://github.com/jasync-sql/jasync-sql/pull/179.

## 1.0.13

* Fix - SuspendingConnection.inTransaction is running on multiple connections [#177](https://github.com/jasync-sql/jasync-sql/issues/177).

## 1.0.12

* R2DBC - upgrade to 0.8 release version https://github.com/jasync-sql/jasync-sql/issues/169
* Kotlin - upgrade to 1.3.61
* Coroutines - upgrade to 1.3.2

## 1.0.11

* R2DBC - Fix a bug that connection factory validation alsways returned false https://github.com/jasync-sql/jasync-sql/issues/161

## 1.0.10

* Fix a bug that when result of query start with empty value there is an exception https://github.com/jasync-sql/jasync-sql/issues/160

## 1.0.9

* Upgrade R2DBC to 0.8.0.RC2, see https://github.com/jasync-sql/jasync-sql/issues/152 and https://github.com/jasync-sql/jasync-sql/issues/159

## 1.0.7

* R2DBC - IllegalStateException: unmatched requested type Object on get("id", Object.class) and Integer-typed value: https://github.com/jasync-sql/jasync-sql/issues/150

## 1.0.6

* MemSQL - Add support for memsql prepared statements: https://github.com/jasync-sql/jasync-sql/issues/140

## 1.0.5

* R2DBC - Allow more type for JasyncInsertSyntheticRow: https://github.com/jasync-sql/jasync-sql/issues/137

## 1.0.4

* R2DBC - add auto generated id returned from server: https://github.com/jasync-sql/jasync-sql/pull/130

(This was broken in 1.0.2)

## 1.0.1

* Fix an issue in case of exception during connection factory test method: https://github.com/jasync-sql/jasync-sql/pull/129

## 1.0.0

* Publish to maven central.

## 0.9.54

* Fix [#124](https://github.com/jasync-sql/jasync-sql/pull/124) - Generated value name should not contain backticks.
* Add [#125](https://github.com/jasync-sql/jasync-sql/pull/124) - Logging Interceptor.

## 0.9.53

* Fix [#121](https://github.com/jasync-sql/jasync-sql/issues/121) - Issue in Stored Procedure call.

## 0.9.52

* Upgrade to r2dbc 0.8.0 M8.
  * Add r2dbc exceptions hierarchy.
  * Add new methods.

## 0.9.51

* Change default protocol encoding to `utf8mb4_unicode_ci`. See: https://github.com/jasync-sql/jasync-sql/pull/115
* Fix timeout issue when connecting to db is slow on pool: https://github.com/jasync-sql/jasync-sql/pull/114
* Fix static method of builder to match getting started.
* Initial Support for `r2dbc`:
  * Add multiple bindings support: https://github.com/jasync-sql/jasync-sql/pull/111
  * Fix conversion for `BigInteger` and `BigDecimal`.
  * Add `Batch` implementation for r2dbc: https://github.com/jasync-sql/jasync-sql/pull/109
  * Support `returnGeneratedValues` for `INSERT` statement: https://github.com/jasync-sql/jasync-sql/pull/108
  * Fixes to r2dbc module: https://github.com/jasync-sql/jasync-sql/pull/104
  * https://github.com/jasync-sql/jasync-sql/issues/98
  * https://github.com/jasync-sql/jasync-sql/pull/99

- Features were released in couple of versions but the above is the version with all fixes.

## 0.9.24

* Fix a leak in mysql driver: [#101](https://github.com/jasync-sql/jasync-sql/pull/101).
* Update netty to `4.1.34.Final`.

## 0.9.23

* Add max ttl for connection: [#96](https://github.com/jasync-sql/jasync-sql/pull/96) and [#97](https://github.com/jasync-sql/jasync-sql/pull/97).

## 0.9.22

* Add logging on connection unregister.

## 0.9.21

* Add sendDirect methods to ConcreteConnection to avoid casting: https://github.com/jasync-sql/jasync-sql/pull/95

## 0.9.20

* Add thread pools to Configuration class instead of directly setting them: https://github.com/jasync-sql/jasync-sql/pull/94

## 0.9.10

All changes in: https://github.com/jasync-sql/jasync-sql/pull/93

* Add support for [interceptors](https://github.com/jasync-sql/jasync-sql/wiki/Interceptors).
* Change: `ConnectionPool` constuctor parameters changed. Class hierarchy for Connections changed: introduced `ConcreteConnection`.
* Support ability to specify coroutines context: https://github.com/jasync-sql/jasync-sql/issues/80

## 0.8.69

* Add logging for timeout.

## 0.8.68

* Add support for application name / client name: https://github.com/jasync-sql/jasync-sql/pull/87

## 0.8.67

* Minor API addition to connection builder.
* Change nullable* extension functions to internal.

## 0.8.66

* Add Prepared statement close support to `SuspendingConnection`.

## 0.8.65

* Fix [Prepared statements are never closed](https://github.com/jasync-sql/jasync-sql/issues/82).
* Add JSON type support to mysql: https://github.com/jasync-sql/jasync-sql/pull/84
* Update versions - Kotlin 1.3.20, Coroutines 1.1.1, netty 4.1.33.

## 0.8.64

* Fix [sporadic NPE in logs](https://github.com/jasync-sql/jasync-sql/issues/59) - suppress error in some more cases.

## 0.8.63

* Fix warnings.

## 0.8.62

* Fix warnings.
* Add epoll support: https://github.com/jasync-sql/jasync-sql/issues/67. See also https://github.com/jasync-sql/jasync-sql/wiki/Native-Transport.

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

See [the older changelog](the-old-files/CHANGELOG-old.md).
