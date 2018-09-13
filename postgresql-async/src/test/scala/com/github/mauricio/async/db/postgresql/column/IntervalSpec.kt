 */

package com.github.mauricio.async.db.postgresql.column

import org.specs2.mutable.Specification

class IntervalSpec : Specification {

  "interval encoder/decoder" should {

    fun decode(s : String) : Any = PostgreSQLIntervalEncoderDecoder.decode(s)
    fun encode(i : Any) : String = PostgreSQLIntervalEncoderDecoder.encode(i)
    fun both(s : String) : String = encode(decode(s))

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
    }.pendingUntilFixed(", mixed/grouped negations")

  }

}