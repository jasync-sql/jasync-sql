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

class VersionSpec extends Specification {

  "version" should {

    "correctly parse versions" in {
      val version = Version("9.1.4")
      version.major === 9
      version.minor === 1
      version.maintenance === 4
    }

    "correctly parse with missing fields" in {
      val version = Version("8.7")
      version.major === 8
      version.minor === 7
      version.maintenance === 0
    }

    "correctly compare between major different versions" in {

      val version1 = Version("8.2.0")
      val version2 = Version("9.2.0")

      version2 must beGreaterThan(version1)

    }

    "correctly compare between major different versions" in {

      val version1 = Version("8.2.0")
      val version2 = Version("8.2.0")

      version2 === version1

    }


    "correctly compare between major different versions" in {

      val version1 = Version("8.2.8")
      val version2 = Version("8.2.87")

      version2 must beGreaterThan( version1 )

    }

    "correctly compare two different versions" in {

      val version1 = Version("9.1.2")
      val version2 = Version("9.2.0")

      version2 must beGreaterThan(version1)

    }

  }

}
