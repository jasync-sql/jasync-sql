package com.github.jasync.sql.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException

class MessageTooLongException(code: Byte, length: Int, limit: Int) :
    DatabaseException("Message of type %d has size %d, higher than the limit %d".format(code, length, limit))
