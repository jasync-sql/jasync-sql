package com.github.mauricio.postgresql

import org.jboss.netty.util.CharsetUtil
import java.nio.charset.Charset

/**
 * User: Maurício Linhares
 * Date: 2/28/12
 * Time: 10:46 PM
 */

object CharsetHelper {

  def toBytes( content : String, charset : Charset ) : Array[Byte] = {
    content.getBytes( charset )
  }

}
