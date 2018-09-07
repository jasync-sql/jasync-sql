# TODO

## Code

- [X] ~todo cleanup in common~
- [X] ~mysql convert~
- [ ] postgres convert
- [ ] tests convert
- [ ] check tests coverage
- [X] ~tests with DB~
- [X] ~kotlin coroutines integration module~ (created an example)
- [ ] Higher level abstarction like https://github.com/scalikejdbc/scalikejdbc-async
- [ ] Fix leaks and PR's on other repos
  - [ ] [performance] https://github.com/mauricio/postgresql-async/pull/231
  - [ ] [postgres] https://github.com/mauricio/postgresql-async/pull/255
  - [ ] [postgres] https://github.com/mauricio/postgresql-async/pull/230
  - [ ] [performance] https://github.com/mauricio/postgresql-async/pull/209
  - [ ] [usability] https://github.com/mauricio/postgresql-async/issues/215
  - [ ] [docs] https://github.com/mauricio/postgresql-async/issues/239
  - [ ] [performance] https://github.com/mauricio/postgresql-async/issues/254
  - [ ] [performance] https://github.com/mauricio/postgresql-async/issues/203
  - [ ] [performance] https://github.com/mauricio/postgresql-async/issues/179
  - [ ] [performance] https://github.com/mauricio/postgresql-async/issues/91
- [X] ~Expose execution context~
- [ ] fix visibility of classes/methods (internal etc')
- [X] ~ob1k integration~
- [ ] remove joda dependency? https://github.com/mauricio/postgresql-async/issues/189
- [ ] todo cleanup
- [ ] create a vertex module: https://vertx.io/docs/vertx-mysql-postgresql-client/java/
- [ ] support PartitionedAsyncObjectPool

## Misc

- [X] ~travis build~
- [X] ~release~
- [X] ~gradle files~
- [ ] old files removal
- [ ] performance benchmark
- [ ] docs, site, logo
- [ ] convert gradle files to Kotlin

# Scala -> Kotlin concepts

* Future -> CompletableFuture
* Seq -> List
* ArrayBuffer -> MutableList
* Scala Duration -> Java 8 Duration
