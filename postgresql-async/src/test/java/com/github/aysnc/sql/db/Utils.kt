package com.github.aysnc.sql.db

import org.assertj.core.api.Assertions

fun verifyException(exType: Class<out java.lang.Exception>,
                            causeType: Class<out java.lang.Exception>? = null,
                            body: () -> Unit): Throwable {
  try {
    body()
    throw Exception("${exType.simpleName}->${causeType?.simpleName} was not thrown")
  } catch (e: Exception) {
    //e.printStackTrace()
    Assertions.assertThat(e::class.java).isEqualTo(exType)
    causeType?.let { Assertions.assertThat(e.cause!!::class.java).isEqualTo(it) }
    return e.cause ?: e
  }
}
