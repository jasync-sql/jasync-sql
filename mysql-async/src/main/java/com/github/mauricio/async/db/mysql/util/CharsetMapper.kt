/*
 * Copyright 2013 Maurício Linhares
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

package com.github.mauricio.async.db.mysql.util

import com.github.mauricio.async.db.mysql.exceptions.CharsetMappingNotAvailableException
import java.nio.charset.Charset
import io.netty.util.CharsetUtil

object CharsetMapper {

  final val Binary = 63

  final val DefaultCharsetsByCharset = Map[Charset,Int](
    CharsetUtil.UTF_8 -> 83,
    CharsetUtil.US_ASCII -> 11,
    CharsetUtil.US_ASCII -> 65,
    CharsetUtil.ISO_8859_1 -> 3,
    CharsetUtil.ISO_8859_1 -> 69
  )

  final val DefaultCharsetsById = DefaultCharsetsByCharset.map { pair => (pair._2, pair._1.name()) }

  final val Instance = new CharsetMapper()
}

class CharsetMapper( charsetsToIntComplement : Map[Charset,Int] = Map.empty[Charset,Int] ) {

  private var charsetsToInt = CharsetMapper.DefaultCharsetsByCharset ++ charsetsToIntComplement

  def toInt( charset : Charset ) : Int = {
    charsetsToInt.getOrElse(charset, {
      throw new CharsetMappingNotAvailableException(charset)
    })
  }

}