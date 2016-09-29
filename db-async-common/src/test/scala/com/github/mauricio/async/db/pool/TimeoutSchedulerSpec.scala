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
package com.github.mauricio.async.db.pool

import java.util.concurrent.{ScheduledFuture, TimeoutException}
import com.github.mauricio.async.db.util.{ByteBufferUtils, ExecutorServiceUtils}
import org.specs2.mutable.SpecificationWithJUnit
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Future, Promise}

/**
 * Tests for TimeoutScheduler
 */
class TimeoutSchedulerSpec extends SpecificationWithJUnit  {

  val TIMEOUT_DID_NOT_PASS = "timeout did not pass"

  "test timeout did not pass" in {
    val timeoutScheduler = new DummyTimeoutScheduler()
    val promise = Promise[String]()
    val scheduledFuture  = timeoutScheduler.addTimeout(promise,Some(Duration(1000, MILLISECONDS)))
    Thread.sleep(100);
    promise.isCompleted === false
    promise.success(TIMEOUT_DID_NOT_PASS)
    Thread.sleep(1500)
    promise.future.value.get.get === TIMEOUT_DID_NOT_PASS
    scheduledFuture.get.isCancelled === true
    timeoutScheduler.timeoutCount === 0
  }

  "test timeout passed" in {
    val timeoutMillis = 100
    val promise = Promise[String]()
    val timeoutScheduler = new DummyTimeoutScheduler()
    val scheduledFuture  = timeoutScheduler.addTimeout(promise,Some(Duration(timeoutMillis, MILLISECONDS)))
    Thread.sleep(1000)
    promise.isCompleted === true
    scheduledFuture.get.isCancelled === false
    promise.trySuccess(TIMEOUT_DID_NOT_PASS)
    timeoutScheduler.timeoutCount === 1
    promise.future.value.get.get must throwA[TimeoutException](message = s"Operation is timeouted after it took too long to return \\(${timeoutMillis} milliseconds\\)")
  }

  "test no timeout" in {
    val timeoutScheduler = new DummyTimeoutScheduler()
    val promise = Promise[String]()
    val scheduledFuture  = timeoutScheduler.addTimeout(promise,None)
    Thread.sleep(1000)
    scheduledFuture === None
    promise.isCompleted === false
    promise.success(TIMEOUT_DID_NOT_PASS)
    promise.future.value.get.get === TIMEOUT_DID_NOT_PASS
    timeoutScheduler.timeoutCount === 0
  }
}

