package com.github.mauricio.async.db.postgresql.exceptions

import com.github.jasync.sql.db.exceptions.DatabaseException

/**
 *
 * Raised if the query string is null or empty.
 *
 * @param query the problematic query
 */
class QueryMustNotBeNullOrEmptyException(query: String) : DatabaseException("Query must not be null or empty, original query is <%s>".format(query))