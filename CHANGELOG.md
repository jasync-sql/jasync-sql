# Changelog

## 0.1.2 (unreleased)

* Optimize match/cases to `@switch` (#10)
* Reimplement the PostgreSQLMD5Digest.java in Scala - (#8)
* Implement MySQL support, should be able to execute common statements and login with password (#9)

## 0.1.1 - 2013-04-30

* Query promises fulfilled before cleaning up the query promise cause the futures to either hang forever or fail with a "query already running" message (#2)
* Optimize MessageEncoder to use a match instead of a map (#3)
* MessageDecoder should validate sizes and correctly handle negative or too large messages (#4)
* Move generic pool classes to the com.github.mauricio.async.db package (#9)

## 0.1.0 - 2013-04-29

* First public release