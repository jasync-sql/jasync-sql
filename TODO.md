# TODO

## Code

- [X] ~todo cleanup in common~
- [X] ~mysql convert~
- [X] ~postgres convert~
- [ ] tests convert
- [ ] check tests coverage
- [X] ~tests with DB~
- [X] ~kotlin coroutines integration module~ (created an example)
- [ ] Fix leaks and PR's on other repos
  - [X] ~[performance] https://github.com/mauricio/postgresql-async/pull/231~
  - [ ] [Postgresql 9.5 Timestamp with time zone] https://github.com/mauricio/postgresql-async/pull/255
  - [X] ~[postgres] https://github.com/mauricio/postgresql-async/pull/230~
  - [ ] [performance] optimize preparedstatement cache memory usage https://github.com/mauricio/postgresql-async/pull/209
  - [X] ~[usability] https://github.com/mauricio/postgresql-async/issues/215~
  - [X] ~[docs] https://github.com/mauricio/postgresql-async/issues/239~
  - [X] ~[performance] https://github.com/mauricio/postgresql-async/issues/254~
  - [ ] [performance] There are no objects available and the waitQueue is full https://github.com/mauricio/postgresql-async/issues/203
  - [X] ~[performance] https://github.com/mauricio/postgresql-async/issues/179~
  - [ ] [performance] Connection pool - Blocking operations https://github.com/mauricio/postgresql-async/issues/91
- [X] ~Expose execution context~
- [ ] fix visibility of classes/methods (internal etc')
- [X] ~ob1k integration~
- [ ] remove joda dependency? https://github.com/mauricio/postgresql-async/issues/189
- [ ] todo cleanup
- [ ] support PartitionedAsyncObjectPool
- [ ] check if we should `ByteBuf.release()` from netty docs, also what happens on inactivate.

## Integrations / Extensions

- [ ] Higher level abstarction like https://github.com/scalikejdbc/scalikejdbc-async or https://github.com/JetBrains/Exposed
- [ ] create a vertex module: https://vertx.io/docs/vertx-mysql-postgresql-client/java/
- [ ] requery integration


## Misc

- [X] ~travis build~
- [X] ~release~
- [X] ~gradle files~
- [ ] old files removal
- [ ] performance benchmark
- [ ] docs, site, logo
- [ ] convert gradle files to Kotlin
- [ ] blog posts for: coroutines, ktor, spring webflux
- [ ] more examples

# Scala -> Kotlin concepts

* Future -> CompletableFuture
* Seq -> List
* ArrayBuffer -> MutableList/Array
* Scala Duration -> Java 8 Duration
