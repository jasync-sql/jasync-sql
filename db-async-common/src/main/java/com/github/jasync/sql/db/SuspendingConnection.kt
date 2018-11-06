package com.github.jasync.sql.db

import kotlinx.coroutines.future.await

val Connection.asSuspending get(): SuspendingConnection = SuspendingConnectionImpl(this)

interface SuspendingConnection {

  /**
   *
   * Disconnects this object. You should discard this object after calling this method. No more queries
   * will be accepted.
   *
   * @return
   */

  suspend fun disconnect(): Connection

  /**
   *
   * Connects this object to the database. Connection objects are not necessarily created , a connection to the
   * database so you might have to call this method to be able to run queries against it.
   *
   * @return
   */

  suspend fun connect(): Connection

  /**
   *
   * Sends a statement to the database. The statement can be anything your database can execute. Not all statements
   * will return a collection of rows, so check the returned object if there are rows available.
   *
   * @param query
   * @return
   */

  suspend fun sendQuery(query: String): QueryResult

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
  suspend fun sendPreparedStatement(query: String, values: List<Any?>): QueryResult

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
  suspend fun sendPreparedStatement(query: String): QueryResult


  /**
   *
   * Executes an (asynchronous) function ,in a transaction block.
   * If the function completes successfully, the transaction is committed, otherwise it is aborted.
   *
   * @param f operation to execute on this connection
   * @return result of f, conditional on transaction operations succeeding
   */

  suspend fun <A> inTransaction(f: suspend (SuspendingConnection) -> A): A

}

class SuspendingConnectionImpl(val connection: Connection): SuspendingConnection {
  /**
   *
   * Disconnects this object. You should discard this object after calling this method. No more queries
   * will be accepted.
   *
   * @return
   */

  override suspend fun disconnect(): Connection = connection.disconnect().await()

  /**
   *
   * Connects this object to the database. Connection objects are not necessarily created , a connection to the
   * database so you might have to call this method to be able to run queries against it.
   *
   * @return
   */

  override suspend fun connect(): Connection = connection.connect().await()

  /**
   *
   * Sends a statement to the database. The statement can be anything your database can execute. Not all statements
   * will return a collection of rows, so check the returned object if there are rows available.
   *
   * @param query
   * @return
   */

  override suspend fun sendQuery(query: String): QueryResult = connection.sendQuery(query).await()

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
  override suspend fun sendPreparedStatement(query: String, values: List<Any?>): QueryResult = connection.sendPreparedStatement(query, values).await()

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
  override suspend fun sendPreparedStatement(query: String): QueryResult = connection.sendPreparedStatement(query).await()


  /**
   *
   * Executes an (asynchronous) function ,in a transaction block.
   * If the function completes successfully, the transaction is committed, otherwise it is aborted.
   *
   * @param f operation to execute on this connection
   * @return result of f, conditional on transaction operations succeeding
   */
  override suspend fun <A> inTransaction(f: suspend (SuspendingConnection) -> A): A {
    try {
      this.sendQuery("BEGIN")
      val result = f(this)
      this.sendQuery("COMMIT")
      return result
    } catch (e: Throwable) {
      this.sendQuery("ROLLBACK")
      throw e
    }

  }

}
