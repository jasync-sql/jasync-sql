/*
 * Copyright 2013 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.jasync.sql.db.pool

import com.github.jasync.sql.db.util.isCompleted
import com.github.jasync.sql.db.util.success
import org.junit.Test
import java.time.Duration
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeoutException
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertNull

/**
 * Tests for TimeoutScheduler
 */
class TimeoutSchedulerSpec {

    val TIMEOUT_DID_NOT_PASS = "timeout did not pass"

    @Test
    fun `test timeout did not pass`() {
        val timeoutScheduler = DummyTimeoutScheduler()
        val promise = CompletableFuture<String>()
        val scheduledFuture = timeoutScheduler.addTimeout(promise, Duration.ofMillis(1000))
        Thread.sleep(100)
        assertFalse(promise.isCompleted)
        promise.success(TIMEOUT_DID_NOT_PASS)
        Thread.sleep(1500)
        assertEquals(TIMEOUT_DID_NOT_PASS, promise.get())
        assertTrue(scheduledFuture!!.isCancelled)
        assertEquals(0, timeoutScheduler.timeOutCount())
    }

    @Test()
    fun `test timeout passed`() {
        val timeoutMillis: Long = 100
        val promise = CompletableFuture<String>()
        val timeoutScheduler = DummyTimeoutScheduler()
        val scheduledFuture = timeoutScheduler.addTimeout(promise, Duration.ofMillis(timeoutMillis))
        Thread.sleep(1000)
        assertTrue(promise.isCompleted)
        assertFalse(scheduledFuture!!.isCancelled)
        promise.success(TIMEOUT_DID_NOT_PASS)
        assertEquals(1, timeoutScheduler.timeOutCount())
        try {
            promise.get()
        } catch (e: ExecutionException) {
            assertNotNull(e.cause)
            assertTrue(e.cause is TimeoutException)
        }
    }

    @Test
    fun `test no timeout`() {
        val timeoutScheduler = DummyTimeoutScheduler()
        val promise = CompletableFuture<String>()
        val scheduledFuture = timeoutScheduler.addTimeout(promise, null)
        Thread.sleep(1000)

        assertNull(scheduledFuture)
        assertFalse(promise.isCompleted)
        promise.success(TIMEOUT_DID_NOT_PASS)
        assertEquals(TIMEOUT_DID_NOT_PASS, promise.get())
        assertEquals(0, timeoutScheduler.timeOutCount())
    }
}
