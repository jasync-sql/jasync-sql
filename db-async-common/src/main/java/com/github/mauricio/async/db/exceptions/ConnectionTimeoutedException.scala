package com.github.mauricio.async.db.exceptions

import com.github.mauricio.async.db.Connection

class ConnectionTimeoutedException( val connection : Connection )
  extends DatabaseException( "The connection %s has a timeouted query and is being closed".format(connection) )