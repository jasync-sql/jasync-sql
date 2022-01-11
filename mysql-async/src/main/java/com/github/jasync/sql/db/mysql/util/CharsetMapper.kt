package com.github.jasync.sql.db.mysql.util

import com.github.jasync.sql.db.mysql.exceptions.CharsetMappingNotAvailableException
import io.netty.util.CharsetUtil
import java.nio.charset.Charset

class CharsetMapper(charsetsToIntComplement: Map<Charset, Int> = emptyMap()) {

    companion object {
        const val Binary = 63

        val DefaultCharsetsByCharset = mapOf(
            CharsetUtil.UTF_8 to Integer.getInteger("jasyncMysqlUTF8Collation", 224), // previous default was 83
            CharsetUtil.US_ASCII to 65,
            CharsetUtil.ISO_8859_1 to 69
        )

        val DefaultCharsetsById = DefaultCharsetsByCharset.map { pair -> (pair.value to pair.key.name()) }

        val Instance = CharsetMapper()
    }

    private var charsetsToInt = CharsetMapper.DefaultCharsetsByCharset + charsetsToIntComplement

    fun toInt(charset: Charset): Int {
        return charsetsToInt.getOrElse(charset) {
            throw CharsetMappingNotAvailableException(charset)
        }
    }
}
