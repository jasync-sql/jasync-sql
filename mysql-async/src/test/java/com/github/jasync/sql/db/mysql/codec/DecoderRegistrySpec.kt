package com.github.jasync.sql.db.mysql.codec

import com.github.jasync.sql.db.mysql.binary.decoder.ByteArrayDecoder
import com.github.jasync.sql.db.mysql.column.ByteArrayColumnDecoder
import com.github.jasync.sql.db.mysql.column.ColumnTypes
import com.github.jasync.sql.db.mysql.util.CharsetMapper
import io.netty.util.CharsetUtil
import org.junit.Test
import kotlin.test.assertSame

class DecoderRegistrySpec {

    @Test
    fun `map FIELD_TYPE_GEOMETRY to byte array decoders`() {
        val registry = DecoderRegistry(CharsetUtil.UTF_8)

        assertSame(
            ByteArrayDecoder,
            registry.binaryDecoderFor(ColumnTypes.FIELD_TYPE_GEOMETRY, CharsetMapper.Binary)
        )
        assertSame(
            ByteArrayColumnDecoder,
            registry.textDecoderFor(ColumnTypes.FIELD_TYPE_GEOMETRY, CharsetMapper.Binary)
        )
    }
}
