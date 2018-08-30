package com.github.mauricio.async.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException

class ByteArrayFormatNotSupportedException : DatabaseException(
    "The bytea 'escape' format is not yet supported, you need to use a PG version that uses the 'hex' format (version 9 and onwards)"
)