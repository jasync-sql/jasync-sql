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

package com.github.mauricio.async.db.mysql.binary

import org.jboss.netty.util.CharsetUtil
import org.specs2.mutable.Specification

class BinaryRowEncoderSpec extends Specification {

  val encoder = new BinaryRowEncoder(CharsetUtil.UTF_8)

  "binary row encoder" should {

    "encode Some(value) like value" in {
      val actual = encoder.encode(List(Some(1l), Some("foo")))
      val expected = encoder.encode(List(1l, "foo"))

      actual mustEqual expected

    }

    "encode None as null" in {
      val actual = encoder.encode(List(None))
      val expected = encoder.encode(List(null))

      actual mustEqual expected
    }

  }

}
