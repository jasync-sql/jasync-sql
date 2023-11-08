package com.github.jasync.sql.db.mysql

import org.assertj.core.api.Assertions

fun verifyException(
    exType: Class<out java.lang.Exception>,
    causeType: Class<out java.lang.Exception>? = null,
    containedInMessage: String? = null,
    body: () -> Unit
): Throwable {
    try {
        body()
        throw Exception("Expected exception was not thrown: ${exType.simpleName}->${causeType?.simpleName}")
    } catch (e: Exception) {
        // e.printStackTrace()
        Assertions.assertThat(e::class.java).isEqualTo(exType)
        if (containedInMessage != null) {
            Assertions.assertThat(e.message).contains(containedInMessage)
        }
        causeType?.let { Assertions.assertThat(e.cause!!::class.java).isEqualTo(it) }
        return e.cause ?: e
    }
}
