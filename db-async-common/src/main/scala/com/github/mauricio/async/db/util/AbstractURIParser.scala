/*
 * Copyright 2016 Maurício Linhares
 *
 * Maurício Linhares licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.github.mauricio.async.db.util

import java.net.{URI, URISyntaxException, URLDecoder}
import java.nio.charset.Charset

import com.github.mauricio.async.db.exceptions.UnableToParseURLException
import com.github.mauricio.async.db.{Configuration, SSLConfiguration}
import org.slf4j.LoggerFactory

import scala.util.matching.Regex

/**
 * Common parser assisting methods for PG and MySQL URI parsers.
 */
abstract class AbstractURIParser {
  import AbstractURIParser._

  protected val logger = LoggerFactory.getLogger(getClass)

  /**
   * Parses out userInfo into a tuple of optional username and password
   *
   * @param userInfo the optional user info string
   * @return a tuple of optional username and password
   */
  final protected def parseUserInfo(userInfo: Option[String]): (Option[String], Option[String]) = userInfo.map(_.split(":", 2).toList) match {
    case Some(user :: pass :: Nil) ⇒ (Some(user), Some(pass))
    case Some(user :: Nil) ⇒ (Some(user), None)
    case _ ⇒ (None, None)
  }

  /**
   * A Regex that will match the base name of the driver scheme, minus jdbc:.
   * Eg: postgres(?:ul)?
   */
  protected val SCHEME: Regex

  /**
   * The default for this particular URLParser, ie: appropriate and specific to PG or MySQL accordingly
   */
  val DEFAULT: Configuration


  /**
   * Parses the provided url and returns a Configuration based upon it.  On an error,
   * @param url the URL to parse.
   * @param charset the charset to use.
   * @return a Configuration.
   */
  @throws[UnableToParseURLException]("if the URL does not match the expected type, or cannot be parsed for any reason")
  def parseOrDie(url: String,
                 charset: Charset = DEFAULT.charset): Configuration = {
    try {
      val properties = parse(new URI(url).parseServerAuthority)

      assembleConfiguration(properties, charset)
    } catch {
      case e: URISyntaxException =>
        throw new UnableToParseURLException(s"Failed to parse URL: $url", e)
    }
  }


  /**
   * Parses the provided url and returns a Configuration based upon it.  On an error,
   * a default configuration is returned.
   * @param url the URL to parse.
   * @param charset the charset to use.
   * @return a Configuration.
   */
  def parse(url: String,
            charset: Charset = DEFAULT.charset
           ): Configuration = {
    try {
      parseOrDie(url, charset)
    } catch {
      case e: Exception =>
        logger.warn(s"Connection url '$url' could not be parsed.", e)
        // Fallback to default to maintain current behavior
        DEFAULT
    }
  }

  /**
   * Assembles a configuration out of the provided property map.  This is the generic form, subclasses may override to
   * handle additional properties.
   * @param properties the extracted properties from the URL.
   * @param charset the charset passed in to parse or parseOrDie.
   * @return
   */
  protected def assembleConfiguration(properties: Map[String, String], charset: Charset): Configuration = {
    DEFAULT.copy(
      username = properties.getOrElse(USERNAME, DEFAULT.username),
      password = properties.get(PASSWORD),
      database = properties.get(DBNAME),
      host = properties.getOrElse(HOST, DEFAULT.host),
      port = properties.get(PORT).map(_.toInt).getOrElse(DEFAULT.port),
      ssl = SSLConfiguration(properties),
      charset = charset
    )
  }


  protected def parse(uri: URI): Map[String, String] = {
    uri.getScheme match {
      case SCHEME() =>
        val userInfo = parseUserInfo(Option(uri.getUserInfo))

        val port = Some(uri.getPort).filter(_ > 0)
        val db = Option(uri.getPath).map(_.stripPrefix("/")).filterNot(_.isEmpty)
        val host = Option(uri.getHost)

        val builder = Map.newBuilder[String, String]
        builder ++= userInfo._1.map(USERNAME -> _)
        builder ++= userInfo._2.map(PASSWORD -> _)
        builder ++= port.map(PORT -> _.toString)
        builder ++= db.map(DBNAME -> _)
        builder ++= host.map(HOST -> unwrapIpv6address(_))

        // Parse query string parameters and just append them, overriding anything previously set
        builder ++= (for {
          qs <- Option(uri.getQuery).toSeq
          parameter <- qs.split('&')
          Array(name, value) = parameter.split('=')
          if name.nonEmpty && value.nonEmpty
        } yield URLDecoder.decode(name, "UTF-8") -> URLDecoder.decode(value, "UTF-8"))


        builder.result
      case "jdbc" =>
        handleJDBC(uri)
      case _ =>
        throw new UnableToParseURLException("Unrecognized URI scheme")
    }
  }

  /**
   * This method breaks out handling of the jdbc: prefixed uri's, allowing them to be handled differently
   * without reimplementing all of parse.
   */
  protected def handleJDBC(uri: URI): Map[String, String] = parse(new URI(uri.getSchemeSpecificPart))


  final protected def unwrapIpv6address(server: String): String = {
    if (server.startsWith("[")) {
      server.substring(1, server.length() - 1)
    } else server
  }

}

object AbstractURIParser {
  // Constants and value names
  val PORT = "port"
  val DBNAME = "database"
  val HOST = "host"
  val USERNAME = "user"
  val PASSWORD = "password"
}

