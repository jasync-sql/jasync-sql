package com.github.jasync.sql.db.postgresql.exceptions

@Suppress("unused")
class ColumnDecoderNotFoundException(kind: Int) :
    IllegalArgumentException("There is no decoder available for kind %s".format(kind))
