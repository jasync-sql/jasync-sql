package com.github.mauricio.postgresql

import org.jboss.netty.util.CharsetUtil

/**
 * User: Maurício Linhares
 * Date: 2/28/12
 * Time: 10:46 PM
 */

object CharsetHelper {

  def toBytes( content : String ) : Array[Byte] = {
    content.getBytes( CharsetUtil.UTF_8 )
  }

}
