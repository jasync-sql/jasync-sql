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

package com.github.mauricio.async.db.postgresql.column

import org.specs2.mutable.Specification

class DefaultColumnEncoderRegistrySpec extends Specification {

  val registry = new PostgreSQLColumnEncoderRegistry()

  "registry" should {

    "correctly render an array of strings with nulls" in {
      val items = Array( "some", """text \ hoes " here to be seen""", null, "all, right" )
      registry.encode( items ) === """{"some","text \\ hoes \" here to be seen",NULL,"all, right"}"""
    }

    "correctly render an array of numbers" in {
      val items = Array(Array(1,2,3),Array(4,5,6),Array(7,null,8))
      registry.encode( items ) === "{{1,2,3},{4,5,6},{7,NULL,8}}"
    }

  }

}
