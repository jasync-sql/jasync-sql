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

object HexCodecSpec {

  final val sampleArray = Array[Byte](83, 97, 121, 32, 72, 101, 108, 108, 111, 32, 116, 111, 32, 77, 121, 32, 76, 105, 116, 116, 108, 101, 32, 70, 114, 105, 101, 110, 100)
  final val sampleHex = "5361792048656c6c6f20746f204d79204c6974746c6520467269656e64".toUpperCase
  final val HexStart = "\\x"
  final val HexStartChars = HexStart.toCharArray

}

class HexCodecSpec extends Specification {

  import HexCodecSpec._

  "codec" should {

    "correctly generate an array of bytes" in {

      val bytes = HexCodec.decode( "5361792048656c6c6f20746f204d79204c6974746c6520467269656e64" )
      bytes === sampleArray

    }

    "correctly generate a string from an array of bytes" in {
      HexCodec.encode(sampleArray) === sampleHex
    }

    "correctly generate a byte array from the PG output" in {

      val input = "\\x53617920"
      val bytes = Array[Byte](83, 97, 121, 32)
      HexCodec.decode(input, 2) === bytes

    }

    "correctly encode to hex using the PostgreSQL format" in {
       HexCodec.encode(sampleArray, HexStartChars) === (HexStart + sampleHex)
    }

  }

}
