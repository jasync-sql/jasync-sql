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

import com.github.jasync.sql.db.util.ExecutorServiceUtils
import com.github.jasync.sql.db.util.NettyUtils
import io.netty.channel.EventLoopGroup
import java.util.concurrent.atomic.AtomicInteger

/**
 * Implementation of TimeoutScheduler used for testing
 */
class DummyTimeoutScheduler : TimeoutSchedulerPartialImpl() {

    val internalPool = ExecutorServiceUtils.CachedThreadPool
    private val timeOuts = AtomicInteger()

    override fun onTimeout() {
        timeOuts.incrementAndGet()
    }

    override fun eventLoopGroup(): EventLoopGroup {
        return NettyUtils.DefaultEventLoopGroup
    }

    fun timeOutCount(): Int {
        return timeOuts.get()
    }

    override fun isTimeout(): Boolean {
        return timeOuts.get() > 0
    }
}
