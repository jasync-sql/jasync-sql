package com.github.jasync.sql.db.exceptions

class ParserNotAvailableException(t: Byte) :
    DatabaseException("There is no parser available for message type '%s' (%s)".format(t, t.toString(16)))
