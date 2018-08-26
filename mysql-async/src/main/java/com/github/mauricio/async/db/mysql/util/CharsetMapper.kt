
package com.github.mauricio.async.db.mysql.util

import com.github.mauricio.async.db.mysql.exceptions.CharsetMappingNotAvailableException
import java.nio.charset.Charset
import io.netty.util.CharsetUtil



class CharsetMapper( charsetsToIntComplement : Map<Charset,Int> = emptyMap() ) {

  companion object {
    val Binary = 63

    val DefaultCharsetsByCharset = mapOf(
        CharsetUtil.UTF_8 to 83,
    CharsetUtil.US_ASCII to 11,
    CharsetUtil.US_ASCII to 65,
    CharsetUtil.ISO_8859_1 to 3,
    CharsetUtil.ISO_8859_1 to 69
    )

    val DefaultCharsetsById = DefaultCharsetsByCharset.map { pair -> (pair.value to  pair.key.name()) }

    val Instance = CharsetMapper()
  }
  private var charsetsToInt = CharsetMapper.DefaultCharsetsByCharset + charsetsToIntComplement

  fun toInt( charset : Charset ) : Int {
    return charsetsToInt.getOrElse(charset) {
      throw CharsetMappingNotAvailableException(charset)
    }
  }

}
