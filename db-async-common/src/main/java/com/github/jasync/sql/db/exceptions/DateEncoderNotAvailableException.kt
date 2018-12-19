package com.github.jasync.sql.db.exceptions

class DateEncoderNotAvailableException(value: Any) :
    DatabaseException("There is no encoder for value <%s> of type %s".format(value, value::class.java.canonicalName))
