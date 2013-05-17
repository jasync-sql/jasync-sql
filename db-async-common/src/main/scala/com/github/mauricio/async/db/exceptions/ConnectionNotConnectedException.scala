package com.github.mauricio.async.db.exceptions

import com.github.mauricio.async.db.Connection

class ConnectionNotConnectedException( val connection : Connection )
  extends DatabaseException( "The connection %s is not connected to the database".format(connection) )