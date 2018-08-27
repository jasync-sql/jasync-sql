
package com.github.jasync.sql.db.mysql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException
import com.github.jasync.sql.db.mysql.message.server.ErrorMessage

class MySQLException( val errorMessage : ErrorMessage )
  : DatabaseException("Error %d - %s - %s".format(errorMessage.errorCode, errorMessage.sqlState, errorMessage.errorMessage))
