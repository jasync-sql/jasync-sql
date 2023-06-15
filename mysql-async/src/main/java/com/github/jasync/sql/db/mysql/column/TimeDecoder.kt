package com.github.jasync.sql.db.mysql.column

import com.github.jasync.sql.db.column.ColumnDecoder
import com.github.jasync.sql.db.util.hours
import com.github.jasync.sql.db.util.length
import com.github.jasync.sql.db.util.millis
import com.github.jasync.sql.db.util.minutes
import com.github.jasync.sql.db.util.seconds
import java.time.Duration

object TimeDecoder : ColumnDecoder {

    override fun decode(value: String): Duration {
        val pieces = value.split(':')

        val secondsAndMillis = pieces[2].split('.')

        val parts = if (secondsAndMillis.length == 2) {
            (secondsAndMillis[0].toInt() to secondsAndMillis[1].toInt())
        } else {
            (secondsAndMillis[0].toInt() to 0)
        }

        val hours = pieces[0].toInt()
        val minutes = pieces[1].toInt()

        return hours.hours + minutes.minutes + parts.first.seconds + parts.second.millis
    }
}
