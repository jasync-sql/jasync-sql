package com.github.jasync.sql.db

import java.util.concurrent.CompletableFuture

/**
 *
 * Base interface for all objects that behave like a connection. This interface will usually be implemented by the
 * objects that connect to a database, either over the filesystem or sockets. {@link Connection} are not supposed
 * to be thread-safe and clients should assume implementations **are not** thread safe and shouldn't try to perform
 * more than one statement (either common or prepared) at the same time. They should wait for the previous statement
 * to be executed to then be able to pick the next one.
 *
 * You can, for instance, compose on top of the futures returned by this class to execute many statements
 * at the same time:
 *
 * {{{
 *   val handler: Connection = ...
 *   val result: Future<QueryResult> = handler.connect
 *     .map(parameters -> handler)
 *     .flatMap(connection -> connection.sendQuery("BEGIN TRANSACTION ISOLATION LEVEL REPEATABLE READ"))
 *     .flatMap(query -> handler.sendQuery("SELECT 0"))
 *     .flatMap(query -> handler.sendQuery("COMMIT").map(value -> query))
 *
 *   val queryResult: QueryResult = Await.result(result, Duration(5, SECONDS))
 * }}}
 *
 */

interface Connection {

    /**
     *
     * Disconnects this object. You should discard this object after calling this method. No more queries
     * will be accepted.
     *
     * @return
     */

    fun disconnect(): CompletableFuture<Connection>

    /**
     *
     * Connects this object to the database. Connection objects are not necessarily created , a connection to the
     * database so you might have to call this method to be able to run queries against it.
     *
     * @return
     */

    fun connect(): CompletableFuture<out Connection>

    /**
     *
     * Checks whether we are still connected to the database.
     *
     * @return
     */

    fun isConnected(): Boolean

    /**
     *
     * Sends a statement to the database. The statement can be anything your database can execute. Not all statements
     * will return a collection of rows, so check the returned object if there are rows available.
     *
     * @param query
     * @return
     */

    fun sendQuery(query: String): CompletableFuture<QueryResult>

    /**
     *
     * Sends a prepared statement to the database. Prepared statements are special statements that are pre-compiled
     * by the database to run faster, they also allow you to avoid SQL injection attacks by not having to concatenate
     * strings from possibly unsafe sources (like users) and sending them directly to the database.
     *
     * When sending a prepared statement, you can insert ? signs in your statement and then provide values at the method
     * call 'values' parameter, as in:
     *
     * {{{
     *  connection.sendPreparedStatement( "SELECT * FROM users WHERE users.login = ?", Array( "john-doe" ) )
     * }}}
     *
     * As you are using the ? as the placeholder for the value, you don't have to perform any kind of manipulation
     * to the value, just provide it as is and the database will clean it up. You must provide as many parameters
     * as you have provided placeholders, so, if your query is as "INSERT INTO users (login,email) VALUES (?,?)" you
     * have to provide an array , at least two values, as in:
     *
     * {{{
     *   Array("john-doe", "doe@mail.com")
     * }}}
     *
     * You can still use this method if your statement doesn't take any parameters, the default is an empty collection.
     *
     * Note about release - It is advised to release the query immediately if it is created dynamically (ie from user input)
     * Otherwise there are the following options:
     * 1. Not release at all - this is good if all prepared statements are known in advance and will not leak
     * 2. Release manually via releasePreparedStatement() - this is usually not recommended and will not work with connection pool
     *
     * @param query
     * @param values
     * @param release - indicate if the prepared statement should be release immediately
     * @return
     */
    fun sendPreparedStatement(query: String, values: List<Any?>, release: Boolean): CompletableFuture<QueryResult>


    /**
     *
     * Sends a prepared statement to the database. Prepared statements are special statements that are pre-compiled
     * by the database to run faster, they also allow you to avoid SQL injection attacks by not having to concatenate
     * strings from possibly unsafe sources (like users) and sending them directly to the database.
     *
     * When sending a prepared statement, you can insert ? signs in your statement and then provide values at the method
     * call 'values' parameter, as in:
     *
     * {{{
     *  connection.sendPreparedStatement( "SELECT * FROM users WHERE users.login = ?", Array( "john-doe" ) )
     * }}}
     *
     * As you are using the ? as the placeholder for the value, you don't have to perform any kind of manipulation
     * to the value, just provide it as is and the database will clean it up. You must provide as many parameters
     * as you have provided placeholders, so, if your query is as "INSERT INTO users (login,email) VALUES (?,?)" you
     * have to provide an array , at least two values, as in:
     *
     * {{{
     *   Array("john-doe", "doe@mail.com")
     * }}}
     *
     * You can still use this method if your statement doesn't take any parameters, the default is an empty collection.
     *
     * @param query
     * @param values
     * @return
     */
    fun sendPreparedStatement(query: String, values: List<Any?>): CompletableFuture<QueryResult> =
        this.sendPreparedStatement(query, values, false)

    /**
     *
     * Sends a prepared statement to the database. Prepared statements are special statements that are pre-compiled
     * by the database to run faster, they also allow you to avoid SQL injection attacks by not having to concatenate
     * strings from possibly unsafe sources (like users) and sending them directly to the database.
     *
     * When sending a prepared statement, you can insert ? signs in your statement and then provide values at the method
     * call 'values' parameter, as in:
     *
     * {{{
     *  connection.sendPreparedStatement( "SELECT * FROM users WHERE users.login = ?", Array( "john-doe" ) )
     * }}}
     *
     * As you are using the ? as the placeholder for the value, you don't have to perform any kind of manipulation
     * to the value, just provide it as is and the database will clean it up. You must provide as many parameters
     * as you have provided placeholders, so, if your query is as "INSERT INTO users (login,email) VALUES (?,?)" you
     * have to provide an array , at least two values, as in:
     *
     * {{{
     *   Array("john-doe", "doe@mail.com")
     * }}}
     *
     * You can still use this method if your statement doesn't take any parameters, the default is an empty collection.
     *
     * @param query
     * @return
     */
    fun sendPreparedStatement(query: String): CompletableFuture<QueryResult> =
        this.sendPreparedStatement(query, emptyList())

    /**
     *
     * Releasing a prepared statement deallocates the data structures kept by the database for this connection
     * with for the given query. You should use this method if you're generating dynamic queries or if the queries
     * you're sending to the database are not going to be reused. Otherwise, it's usually faster to leave the queries
     * you have already built in place, they are released when the connection is closed.
     *
     * @param query the query that produced the prepared statement that is to be released.
     * @return a {@link scala.concurrent.Future} with a true or false indicating if the query existed or not.
     */

    fun releasePreparedStatement(query : String) : CompletableFuture<Boolean>

    /**
     *
     * Executes an (asynchronous) function ,in a transaction block.
     * If the function completes successfully, the transaction is committed, otherwise it is aborted.
     *
     * @param f operation to execute on this connection
     * @return result of f, conditional on transaction operations succeeding
     */

    fun <A> inTransaction(f: (Connection) -> CompletableFuture<A>): CompletableFuture<A>

}
