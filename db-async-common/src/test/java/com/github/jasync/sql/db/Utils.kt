package com.github.jasync.sql.db

import org.assertj.core.api.Assertions

fun verifyException(exceptionType: Class<out java.lang.Exception>,
                    causeType: Class<out java.lang.Exception>? = null,
                    body: () -> Unit) {
  try {
    body()
    throw Exception("${exceptionType.simpleName}->${causeType?.simpleName} was not thrown")
  } catch (e: Exception) {
    Assertions.assertThat(e::class.java).isEqualTo(exceptionType)
    causeType?.let { Assertions.assertThat(e.cause!!::class.java).isEqualTo(it) }
  }
}
