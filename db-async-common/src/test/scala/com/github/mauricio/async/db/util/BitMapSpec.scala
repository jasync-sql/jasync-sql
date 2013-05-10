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

class BitMapSpec extends Specification {

  "bitmap" should {

    "correctly set and unset bits" in {

      val bitMap = new BitMap(0x32, 121)

      bitMap.isSet(0) must beFalse
      bitMap.isSet(1) must beFalse
      bitMap.isSet(2) must beTrue
      bitMap.isSet(3) must beTrue
      bitMap.isSet(4) must beFalse
      bitMap.isSet(5) must beFalse
      bitMap.isSet(6) must beTrue
      bitMap.isSet(7) must beFalse

      bitMap.toString === "0011001001111001"

    }

  }

}
