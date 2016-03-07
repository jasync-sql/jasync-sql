package com.github.mauricio.async.db.postgresql

import org.specs2.mutable.Specification
import com.github.mauricio.async.db.SSLConfiguration.Mode
import javax.net.ssl.SSLHandshakeException

class PostgreSQLSSLConnectionSpec extends Specification with DatabaseTestHelper {

  "ssl handler" should {

    "connect to the database in ssl without verifying CA" in {

      withSSLHandler(Mode.Require, "127.0.0.1", None) { handler =>
        handler.isReadyForQuery must beTrue
      }

    }

    "connect to the database in ssl verifying CA" in {

      withSSLHandler(Mode.VerifyCA, "127.0.0.1") { handler =>
        handler.isReadyForQuery must beTrue
      }

    }

    "connect to the database in ssl verifying CA and hostname" in {

      withSSLHandler(Mode.VerifyFull) { handler =>
        handler.isReadyForQuery must beTrue
      }

    }

    "throws exception when CA verification fails" in {

      withSSLHandler(Mode.VerifyCA, rootCert = None) { handler =>
      } must throwA[SSLHandshakeException]

    }

    "throws exception when hostname verification fails" in {

      withSSLHandler(Mode.VerifyFull, "127.0.0.1") { handler =>
      } must throwA[SSLHandshakeException]

    }

  }

}
