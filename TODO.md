# TODO

## Code

- [X] ~todo cleanup in common~
- [X] ~mysql convert~
- [X] ~postgres convert~
- [X] ~tests convert~
- [X] ~check tests coverage~
- [X] ~tests with DB~
- [X] ~kotlin coroutines integration module (created an example)~
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
- [ ] remove joda dependency https://github.com/mauricio/postgresql-async/issues/189 https://www.google.co.il/search?q=migrate+joda+to+java+8&oq=migrate+joda+to+java+8
- [ ] todo cleanup
- [X] ~support PartitionedAsyncObjectPool~
- [X] ~support ActorBasedAsyncObjectPool~
- [X] ~check if we should `ByteBuf.release()` from netty docs, also what happens on inactivate. Not needed because we use `SimpleChannelHandler` more details here: https://netty.io/wiki/reference-counted-objects.html~
- [ ] Add more debug logging for result from queries etc'.

## Integrations / Extensions / Samples

- [ ] Higher level abstarction like https://github.com/scalikejdbc/scalikejdbc-async or https://github.com/JetBrains/Exposed
- [ ] create a vertex module: https://vertx.io/docs/vertx-mysql-postgresql-client/java/ https://github.com/vert-x3/vertx-mysql-postgresql-client
- [ ] requery integration
- [X] ~MariaDB~
- [ ] Micronaut
- [ ] Spark Java


## Misc

- [X] ~travis build~
- [X] ~release~
- [X] ~gradle files~
- [X] ~old files removal~
- [ ] performance benchmark
- [X] ~site, logo~
- [X] ~apiari style docs~
- [ ] convert gradle files to Kotlin
- [ ] blog posts for: ~coroutines, ktor,~ spring webflux
- [ ] more examples

# Scala -> Kotlin concepts

* Future -> CompletableFuture
* Seq -> List
* ArrayBuffer -> MutableList/Array
* Scala Duration -> Java 8 Duration
