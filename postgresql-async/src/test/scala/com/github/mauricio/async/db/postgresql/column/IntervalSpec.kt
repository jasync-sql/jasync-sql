/*
 * Copyright 2013 Maurício Linhares
 * Copyright 2013 Dylan Simon
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

class IntervalSpec extends Specification {

  "interval encoder/decoder" should {

    def decode(s : String) : Any = PostgreSQLIntervalEncoderDecoder.decode(s)
    def encode(i : Any) : String = PostgreSQLIntervalEncoderDecoder.encode(i)
    def both(s : String) : String = encode(decode(s))

    "parse and encode example intervals" in {
      Seq("1-2", "1 year 2 mons", "@ 1 year 2 mons", "@ 1 year 2 mons", "P1Y2M") forall {
        both(_) === "P1Y2M"
      }
      Seq("3 4:05:06", "3 days 04:05:06", "@ 3 days 4 hours 5 mins 6 secs", "P3DT4H5M6S") forall {
        both(_) === "P3DT4H5M6S"
      }
      Seq("1-2 +3 4:05:06", "1 year 2 mons +3 days 04:05:06", "@ 1 year 2 mons 3 days 4 hours 5 mins 6 secs", "P1Y2M3DT4H5M6S") forall {
        both(_) === "P1Y2M3DT4H5M6S"
      }
      Seq("@ 1 year 2 mons -3 days 4 hours 5 mins 6 secs ago", "P-1Y-2M3DT-4H-5M-6S") forall {
        both(_) === "P-1Y-2M3DT-4H-5M-6S"
      }
      both("-1.234") === "PT-1.234S"
      both("-4:05:06") === "PT-4H-5M-6S"
    }

    "parse and encode example intervals" in {
      Seq("-1-2 +3 -4:05:06", "-1 year -2 mons +3 days -04:05:06") forall {
        both(_) === "P-1Y-2M3DT-4H-5M-6S"
      }
    }.pendingUntilFixed("with mixed/grouped negations")

  }

}
