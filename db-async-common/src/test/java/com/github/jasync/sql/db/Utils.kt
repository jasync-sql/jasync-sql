package com.github.jasync.sql.db

import org.assertj.core.api.Assertions

fun verifyExceptionInHierarchy(
    exceptionType: Class<out java.lang.Exception>,
    body: () -> Unit
) {
    try {
        body()
        throw IllegalStateException("${exceptionType.simpleName} was not thrown")
    } catch (e: Exception) {
        var cause: Throwable? = e
        while (cause != null) {
            if (cause.javaClass == exceptionType) {
                return
            }
            cause = cause.cause
        }
        e.printStackTrace()
        Assertions.assertThat(e::class.java).isEqualTo(exceptionType)
    }
}

fun verifyException(
    exceptionType: Class<out java.lang.Exception>,
    causeType: Class<out java.lang.Exception>? = null,
    body: () -> Unit
) {
    try {
        body()
        throw Exception("${exceptionType.simpleName}->${causeType?.simpleName} was not thrown")
    } catch (e: Exception) {
        Assertions.assertThat(e::class.java).isEqualTo(exceptionType)
        causeType?.let { Assertions.assertThat(e.cause::class.java).isEqualTo(it) }
    }
}
