/**
 *
 */
package com.github.mauricio.async.db.postgresql.util

import org.slf4j.LoggerFactory

/**
 * @author gciuloaica
 *
 */
object ParserURL {

  private val logger = LoggerFactory.getLogger(ParserURL.getClass())

  val PGPORT = "port"
  val PGDBNAME = "database"
  val PGHOST = "host"
  val PGUSERNAME = "username"
  val PGPASSWORD = "password"

  val DEFAULT_PORT = "5432"

  private val pgurl1 = """(jdbc:postgresql)://([^:]*|\[.+\])(?::(\d+))?/([^?]+)(?:\?user=(.*)&password=(.*))?""".r
  private val pgurl2 = """(postgres|postgresql)://(.*):(.*)@(.*):(\d+)/(.*)""".r

  def parse(connectionURL: String): Map[String, String] = {
    val properties: Map[String, String] = Map()

    connectionURL match {
      case pgurl1(protocol, server, port, dbname, username, password) => {
        var result = properties + (PGHOST -> unwrapIpv6address(server)) + (PGDBNAME -> dbname)
        if(port != null) result += (PGPORT -> port)
        if(username != null) result = (result + (PGUSERNAME -> username) + (PGPASSWORD -> password))
        result
      }
      case pgurl2(protocol, username, password, server, port, dbname) => {
        properties + (PGHOST -> unwrapIpv6address(server)) + (PGPORT -> port) + (PGDBNAME -> dbname) + (PGUSERNAME -> username) + (PGPASSWORD -> password)
      }
      case _ => {
        logger.warn(s"Connection url '$connectionURL' could not be parsed.")
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