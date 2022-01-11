package com.github.aysnc.sql.db.postgresql.column

import com.github.jasync.sql.db.postgresql.column.PostgreSQLIntervalEncoderDecoder
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

class IntervalSpec {

    private fun decode(s: String): Any = PostgreSQLIntervalEncoderDecoder.decode(s)
    private fun encode(i: Any): String = PostgreSQLIntervalEncoderDecoder.encode(i)
    private fun both(s: String): String = encode(decode(s))

    @Test
    @Ignore // .pendingUntilFixed(", mixed/grouped negations")
    fun `interval encoder decoder should parse and encode example intervals`() {
        listOf("1-2", "1 year 2 mons", "@ 1 year 2 mons", "@ 1 year 2 mons", "P1Y2M").forEach {
            assertThat(both(it)).isEqualTo("P1Y2M")
        }
        listOf("3 4:05:06", "3 days 04:05:06", "@ 3 days 4 hours 5 mins 6 secs", "P3DT4H5M6S").forEach {
            assertThat(both(it)).isEqualTo("P3DT4H5M6S")
        }
        listOf(
            "1-2 +3 4:05:06",
            "1 year 2 mons +3 days 04:05:06",
            "@ 1 year 2 mons 3 days 4 hours 5 mins 6 secs",
            "P1Y2M3DT4H5M6S"
        ).forEach {
            assertThat(both(it)).isEqualTo("P1Y2M3DT4H5M6S")
        }
        listOf("@ 1 year 2 mons -3 days 4 hours 5 mins 6 secs ago", "P-1Y-2M3DT-4H-5M-6S").forEach {
            assertThat(both(it)).isEqualTo("P-1Y-2M3DT-4H-5M-6S")
        }
        assertThat(both("-1.234")).isEqualTo("PT-1.234S")
        assertThat(both("-4:05:06")).isEqualTo("PT-4H-5M-6S")
    }

    @Test
    @Ignore // .pendingUntilFixed(", mixed/grouped negations")
    fun `interval encoder decoder should parse and encode example intervals2`() {
        listOf("-1-2 +3 -4:05:06", "-1 year -2 mons +3 days -04:05:06").forEach {
            assertThat(both(it)).isEqualTo("P-1Y-2M3DT-4H-5M-6S")
        }
    }
}
