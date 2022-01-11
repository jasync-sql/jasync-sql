package com.github.jasync.r2dbc.mysql

private val milliToNanoConst = 1000000

/**
 * Converts Joda-Time LocalDate to Java 8 equivalent.
 *
 * @param this Joda-Time LocalDate
 * @return Java 8 LocalDate
 */
fun org.joda.time.LocalDate.jodaToJavaLocalDate(): java.time.LocalDate {
    return java.time.LocalDate.of(this.year, this.monthOfYear, this.dayOfMonth)
}

/**
 * Converts Joda-Time LocalTime to Java 8 equivalent.
 *
 * @param this Joda-Time LocalTime
 * @return Java 8 LocalTime
 */
fun org.joda.time.LocalTime.jodaToJavaLocalTime(): java.time.LocalTime {
    return java.time.LocalTime.of(
        this.hourOfDay,
        this.minuteOfHour,
        this.secondOfMinute,
        this.millisOfSecond * milliToNanoConst
    )
}

/**
 * Converts Joda-Time LocalDateTime to Java 8 equivalent.
 *
 * @param this Joda-Time LocalDateTime
 * @return Java 8 LocalDateTime
 */
fun org.joda.time.LocalDateTime.jodaToJavaLocalDateTime(): java.time.LocalDateTime {
    return java.time.LocalDateTime.of(
        this.year,
        this.monthOfYear,
        this.dayOfMonth,
        this.hourOfDay,
        this.minuteOfHour,
        this.secondOfMinute,
        this.millisOfSecond * milliToNanoConst
    )
}
