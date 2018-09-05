package com.github.jasync.sql.db.util

import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

val Long.days: Duration get() = Duration.ofDays(this)
val Long.hours: Duration get() = Duration.ofHours(this)
val Long.minutes: Duration get() = Duration.ofMinutes(this)
val Long.seconds: Duration get() = Duration.ofSeconds(this)
val Long.micros: Duration get() = Duration.of(this, ChronoUnit.MICROS)
val Int.hour: Duration get() = Duration.ofHours(this.toLong())
val Int.hours: Duration get() = Duration.ofHours(this.toLong())
val Int.minutes: Duration get() = Duration.ofMinutes(this.toLong())
val Int.seconds: Duration get() = Duration.ofSeconds(this.toLong())
val Int.millis: Duration get() = Duration.ofMillis(this.toLong())
val Short.hours: Duration get() = Duration.ofHours(this.toLong())
val Short.minutes: Duration get() = Duration.ofMinutes(this.toLong())
val Short.seconds: Duration get() = Duration.ofSeconds(this.toLong())
val Duration.micros: Long get() = TimeUnit.NANOSECONDS.toMicros(this.toNanos())
fun Duration.neg(): Duration = this.negated()
