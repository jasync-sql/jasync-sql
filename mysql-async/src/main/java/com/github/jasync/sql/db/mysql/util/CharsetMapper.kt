package com.github.jasync.sql.db.mysql.util

import com.github.jasync.sql.db.mysql.exceptions.CharsetMappingNotAvailableException
import java.nio.charset.Charset
import io.netty.util.CharsetUtil


class CharsetMapper(charsetsToIntComplement: Map<Charset, Int> = emptyMap()) {

    companion object {
        val Binary = 63

        val DefaultCharsetsByCharset = mapOf(
            CharsetUtil.UTF_8 to 33,
            CharsetUtil.US_ASCII to 11,
            CharsetUtil.ISO_8859_1 to 8  //same latin1
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
