package com.github.jasync.sql.db.exceptions

import com.github.jasync.sql.db.Connection

class ConnectionTimeoutedException( val connection : Connection )
  : DatabaseException( "The connection %s has a timeouted query and is being closed".format(connection) )
