package com.github.jasync.sql.db.util

import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder

internal const val ALL_MICROS_FORMAT = ".[SSSSSS][SSSSS][SSSS][SSS][SS][S]"

val microsecondsFormatter: DateTimeFormatter = DateTimeFormatterBuilder()
    .appendPattern(ALL_MICROS_FORMAT).toFormatter()
