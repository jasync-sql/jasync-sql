
package com.github.mauricio.async.db.mysql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException
import com.github.mauricio.async.db.mysql.message.server.ErrorMessage

class MySQLException( val errorMessage : ErrorMessage )
  : DatabaseException("Error %d - %s - %s".format(errorMessage.errorCode, errorMessage.sqlState, errorMessage.errorMessage))
