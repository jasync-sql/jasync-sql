
package com.github.mauricio.async.db.exceptions

class NegativeMessageSizeException( code : Byte, size : Int )
  : DatabaseException( "Message of type %d had negative size %s".format(code, size) )