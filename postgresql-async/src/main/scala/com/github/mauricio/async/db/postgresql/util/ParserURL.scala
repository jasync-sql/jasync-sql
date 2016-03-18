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
  val PGUSERNAME = "user"
  val PGPASSWORD = "password"

  val DEFAULT_PORT = "5432"

  private val pgurl1 = """(jdbc:postgresql):(?://([^/:]*|\[.+\])(?::(\d+))?)?(?:/([^/?]*))?(?:\?(.*))?""".r
  private val pgurl2 = """(postgres|postgresql)://(.*):(.*)@(.*):(\d+)/([^/?]*)(?:\?(.*))?""".r

  def parse(connectionURL: String): Map[String, String] = {
    val properties: Map[String, String] = Map()

    def parseOptions(optionsStr: String): Map[String, String] =
      optionsStr.split("&").map { o =>
        o.span(_ != '=') match {
          case (name, value) => name -> value.drop(1)
        }
      }.toMap

    connectionURL match {
      case pgurl1(protocol, server, port, dbname, params) => {
        var result = properties
        if (server != null) result += (PGHOST -> unwrapIpv6address(server))
        if (dbname != null && dbname.nonEmpty) result += (PGDBNAME -> dbname)
        if (port != null) result += (PGPORT -> port)
        if (params != null) result ++= parseOptions(params)
        result
      }
      case pgurl2(protocol, username, password, server, port, dbname, params) => {
        var result = properties + (PGHOST -> unwrapIpv6address(server)) + (PGPORT -> port) + (PGDBNAME -> dbname) + (PGUSERNAME  -> username) + (PGPASSWORD  -> password)
        if (params != null) result ++= parseOptions(params)
        result
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
