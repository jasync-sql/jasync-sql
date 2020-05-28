package com.github.aysnc.sql.db.postgresql.column

import com.github.jasync.sql.db.column.IntegerEncoderDecoder
import com.github.jasync.sql.db.postgresql.column.ArrayDecoder
import io.mockk.mockk
import io.netty.buffer.Unpooled
import io.netty.util.CharsetUtil
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ArrayDecoderSpec {

    fun execute(data: String): Any? {
        val numbers = data.toByteArray(CharsetUtil.UTF_8)
        val encoder = ArrayDecoder(IntegerEncoderDecoder)
        return encoder.decode(mockk(), Unpooled.wrappedBuffer(numbers), CharsetUtil.UTF_8)
    }

    @Test
    fun `"encoder decoder" should "parse an array of numbers"`() {

        assertThat(execute("{1,2,3}")).isEqualTo(listOf(1, 2, 3))
    }

    @Test
    fun `"encoder decoder" should "parse an array of array of numbers"`() {
        assertThat(execute("{{1,2,3},{4,5,6}}")).isEqualTo(listOf(listOf(1, 2, 3), listOf(4, 5, 6)))
    }
}
