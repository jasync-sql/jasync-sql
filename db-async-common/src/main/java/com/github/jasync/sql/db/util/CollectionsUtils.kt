package com.github.jasync.sql.db.util

val <T> List<T>.tail: List<T>
  get() = drop(1)

val <T> List<T>.head: T
  get() = first()

val <T> List<T>.headTail: Pair<T, List<T>>
  get() = this.head to this.tail

val <T> Collection<T>.length get() = this.size
val ByteArray.length get() = this.size
