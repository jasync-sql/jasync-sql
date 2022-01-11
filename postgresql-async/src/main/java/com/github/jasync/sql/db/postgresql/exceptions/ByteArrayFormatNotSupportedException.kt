package com.github.jasync.sql.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException

@Suppress("unused")
class ByteArrayFormatNotSupportedException : DatabaseException(
    "The bytea 'escape' format is not yet supported, you need to use a PG version that uses the 'hex' format (version 9 and onwards)"
)
