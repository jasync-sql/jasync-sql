
package com.github.mauricio.async.db.exceptions

class ConnectionStillRunningQueryException( connectionCount : Long, caughtRace : Boolean)
  : DatabaseException ( "<%s> - There is a query still being run here - race -> %s".format(
    connectionCount,
    caughtRace
  ))