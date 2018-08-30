package com.github.mauricio.async.db.postgresql.exceptions

class ColumnDecoderNotFoundException(kind: Int) : IllegalArgumentException("There is no decoder available for kind %s".format(kind))
