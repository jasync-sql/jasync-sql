package com.github.jasync.r2dbc.mysql

import io.mockk.mockk
import io.r2dbc.spi.R2dbcTimeoutException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.CompletionException
import java.util.concurrent.TimeoutException

class JasyncStatementExceptionMappingTest {

    @Test
    fun `maps TimeoutException and CompletionException(TimeoutException) to R2dbcTimeoutException`() {
        val statement = JasyncStatement(mockk(), "SELECT 1")
        val timeout = TimeoutException("Operation timeout after it took too long to return (PT3S)")

        val direct = statement.mapException(timeout)
        assertThat(direct)
            .isInstanceOf(R2dbcTimeoutException::class.java)
            .hasMessage(timeout.message)
            .hasCause(timeout)

        val wrapped = statement.mapException(CompletionException(timeout))
        assertThat(wrapped)
            .isInstanceOf(R2dbcTimeoutException::class.java)
            .hasMessage(timeout.message)
            .hasCause(timeout)
    }

    @Test
    fun `preserves exception message on fallback JasyncDatabaseException`() {
        val statement = JasyncStatement(mockk(), "SELECT 1")
        val failure = RuntimeException("unexpected failure details")

        val mapped = statement.mapException(CompletionException(failure))
        assertThat(mapped)
            .isInstanceOf(JasyncDatabaseException::class.java)
            .hasMessage("unexpected failure details")
            .hasCause(failure)
        assertThat((mapped as JasyncDatabaseException).sqlState).isEqualTo("UNKNOWN")
    }
}
