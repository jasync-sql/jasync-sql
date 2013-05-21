/**
 *
 */
package com.github.mauricio.async.db.postgresql.util

/**
 * @author gciuloaica
 *
 */
object ParserURL {

  val PGPORT = "port"
  val PGDBNAME = "database"
  val PGHOST = "host"
  val PGUSERNAME = "username"
  val PGPASSWORD = "password"

  val DEFAULT_PORT = "5234"

  def parse(connectionURL: String): Map[String, String] = {
    val properties: Map[String, String] = Map()
    val pgurl1 = """(jdbc:postgresql)://(.*):(\d+)/(.*)\?username=(.*)&password=(.*)""".r
    val pgurl2 = """(postgresql)://(.*):(.*)@(.*):(\d+)/(.*)""".r

    if (connectionURL.startsWith("jdbc")) {
      connectionURL match {
        case pgurl1(protocol, server, port, dbname, username, password) => {
          properties + (PGHOST -> unwrapIpv6address(server)) + (PGPORT -> port) + (PGDBNAME -> dbname) + (PGUSERNAME -> username) + (PGPASSWORD -> password)
        }
      }

    } else {

      if (connectionURL.startsWith("postgresql")) {
        connectionURL match {
          case pgurl2(protocol, username, password, server, port, dbname) => {
            properties + (PGHOST -> unwrapIpv6address(server)) + (PGPORT -> port) + (PGDBNAME -> dbname) + (PGUSERNAME -> username) + (PGPASSWORD -> password)
          }
        }
      } else {
        properties
      }
    }

  }

  private def unwrapIpv6address(server: String): String = {
    if (server.startsWith("[")) {
      server.substring(1, server.length() - 1)
    } else server
  }

}