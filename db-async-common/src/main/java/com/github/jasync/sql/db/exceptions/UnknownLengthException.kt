
package com.github.jasync.sql.db.exceptions

class UnknownLengthException ( length : Int )
  : DatabaseException( "Can't handle the length %d".format(length) )
