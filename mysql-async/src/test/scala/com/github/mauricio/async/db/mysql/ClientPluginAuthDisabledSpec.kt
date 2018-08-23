package com.github.mauricio.async.db.mysql

import com.github.mauricio.async.db.Configuration
import org.specs2.mutable.Specification

/**
 *
 * To run this spec you have to use the Vagrant file provided with the base project
 * and you have to start MySQL there. The expected MySQL version is 5.1.73.
 * Make sure the bootstrap.sh script is run, if it isn't, manually run it yourself.
 *
 */

class ClientPluginAuthDisabledSpec extends Specification with ConnectionHelper {

  "connection" should {

    "connect and query the database without a password" in {

      if (System.getenv("TRAVIS") == null) {
        withConnection {
          connection =>
            executeQuery(connection, "select version()")
            success("did work")
        }
      } else {
        skipped("not to be run on travis")
      }

    }

    "connect and query the database with a password" in {

      if (System.getenv("TRAVIS") == null) {
        withConfigurableConnection(vagrantConfiguration) {
          connection =>
            executeQuery(connection, "select version()")
            success("did work")
        }
      } else {
        skipped("not to be run on travis")
      }

    }

  }

  override def defaultConfiguration = new Configuration(
    "root",
    "localhost",
    port = 3307
  )

  def vagrantConfiguration = new Configuration(
    "mysql_vagrant",
    "localhost",
    port = 3307,
    password = Some("generic_password")
  )

}
