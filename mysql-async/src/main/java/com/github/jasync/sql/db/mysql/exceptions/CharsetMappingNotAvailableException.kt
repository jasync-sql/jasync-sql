package com.github.jasync.sql.db.mysql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException
import java.nio.charset.Charset

class CharsetMappingNotAvailableException(charset: Charset) :
    DatabaseException("There is no MySQL charset mapping name for the Java Charset ${charset.name()}")
