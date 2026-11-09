package com.github.jasync.sql.db.mysql.codec

import com.github.jasync.sql.db.column.StringEncoderDecoder
import com.github.jasync.sql.db.mysql.binary.decoder.ByteArrayDecoder
import com.github.jasync.sql.db.mysql.column.ByteArrayColumnDecoder
import com.github.jasync.sql.db.mysql.column.ColumnTypes
import com.github.jasync.sql.db.mysql.util.CharsetMapper
import io.netty.util.CharsetUtil
import org.junit.Test
import kotlin.test.assertSame

class DecoderRegistrySpec {

    @Test
    fun `map FIELD_TYPE_GEOMETRY and binary BLOB variants to byte array decoders`() {
        val registry = DecoderRegistry(CharsetUtil.UTF_8)

        assertSame(
            ByteArrayDecoder,
            registry.binaryDecoderFor(ColumnTypes.FIELD_TYPE_GEOMETRY, CharsetMapper.Binary)
        )
        assertSame(
            ByteArrayColumnDecoder,
            registry.textDecoderFor(ColumnTypes.FIELD_TYPE_GEOMETRY, CharsetMapper.Binary)
        )

        val utf8Code = CharsetMapper.Instance.toInt(CharsetUtil.UTF_8)
        listOf(
            ColumnTypes.FIELD_TYPE_BLOB,
            ColumnTypes.FIELD_TYPE_LONG_BLOB,
            ColumnTypes.FIELD_TYPE_MEDIUM_BLOB,
            ColumnTypes.FIELD_TYPE_TINY_BLOB
        ).forEach { blobType ->
            assertSame(ByteArrayColumnDecoder, registry.textDecoderFor(blobType, CharsetMapper.Binary))
            assertSame(StringEncoderDecoder, registry.textDecoderFor(blobType, utf8Code))
        }
    }
}
