package com.github.aysnc.sql.db.integration

import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.SSLConfiguration
import java.util.concurrent.ExecutionException
import javax.net.ssl.SSLHandshakeException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class PostgreSQLSSLConnectionSpec : DatabaseTestHelper() {

    @Test
    fun `"ssl handler" should "connect to the database in ssl without verifying CA" `() {
        withSSLHandler(SSLConfiguration.Mode.Require, "127.0.0.1", null) { handler ->
            assertThat(handler.isReadyForQuery()).isTrue()
        }
    }

    @Test
    fun `"ssl handler" should "connect to the database in ssl verifying CA" `() {
        withSSLHandler(SSLConfiguration.Mode.VerifyCA, "127.0.0.1") { handler ->
            assertThat(handler.isReadyForQuery()).isTrue()
        }
    }

    @Test
    fun `"ssl handler" should "connect to the database in ssl verifying CA and hostname" `() {
        withSSLHandler(SSLConfiguration.Mode.VerifyFull) { handler ->
            assertThat(handler.isReadyForQuery()).isTrue()
        }
    }

    @Test
    fun `"ssl handler" should "throws exception when CA verification fails" `() {
        verifyException(ExecutionException::class.java, SSLHandshakeException::class.java) {
            withSSLHandler(SSLConfiguration.Mode.VerifyCA, rootCert = null) {
            }
        }
    }

    @Test
    fun `"ssl handler" should  "throws exception when hostname verification fails"  `() {
        verifyException(ExecutionException::class.java, SSLHandshakeException::class.java) {
            withSSLHandler(SSLConfiguration.Mode.VerifyFull, "127.0.0.1") {
            }
        }
    }
}
