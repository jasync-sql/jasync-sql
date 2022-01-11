package com.github.jasync.sql.db.exceptions

@Suppress("RedundantVisibilityModifier")
public class DateEncoderNotAvailableException(value: Any) :
    DatabaseException("There is no encoder for value <%s> of type %s".format(value, value::class.java.canonicalName))
