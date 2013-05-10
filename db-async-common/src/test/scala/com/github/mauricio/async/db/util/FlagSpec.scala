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

package com.github.mauricio.async.db.util

import org.specs2.mutable.Specification

class FlagSpec extends Specification {

  import SampleFlag._

  "flag" should {

    "compose on flags correctly" in {

      val flag = new SampleFlag(0) + Flag_1
      flag.has(Flag_1) must beTrue

    }

  }

}

object SampleFlag {

  val Flag_1 = 0x0080
  val Flag_2 = 0x0100
  val Flag_3 = 0x0200

  val FlagMap = Map(
    "Flag_1" -> Flag_1,
    "Flag_2" -> Flag_2,
    "Flag_3" -> Flag_3
  )

}

class SampleFlag( flag : Int ) extends Flag[SampleFlag]( flag, SampleFlag.FlagMap ) {
  protected def create(value: Int): SampleFlag = new SampleFlag(value)
}