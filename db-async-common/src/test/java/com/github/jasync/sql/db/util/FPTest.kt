package com.github.jasync.sql.db.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.ExecutionException

class FPTest {

    @Test(expected = ExecutionException::class)
    fun failed() {
        val future = FP.failed<String>(Exception())
        future.get()
    }

    @Test
    fun successful() {
        val future = FP.successful("a")
        assertThat(future.get()).isEqualTo("a")
        assertThat(future.isSuccess).isTrue()
        assertThat(future.isFailure).isFalse()
    }

    @Test
    fun `test flatMap`() {
        assertThat(FP.successful("a").flatMap { FP.successful("$it$it") }.get()).isEqualTo("aa")
    }

    @Test
    fun `test onComplete`() {
        var completed = false
        assertThat(FP.successful("a").onComplete { completed = true }.get()).isEqualTo("a")
        assertThat(completed).isTrue()
    }
}
