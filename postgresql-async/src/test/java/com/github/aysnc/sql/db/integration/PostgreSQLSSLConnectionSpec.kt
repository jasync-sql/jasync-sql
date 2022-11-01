package com.github.aysnc.sql.db.integration

import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.SSLConfiguration
import io.netty.handler.ssl.util.SelfSignedCertificate
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.util.concurrent.ExecutionException
import javax.net.ssl.SSLHandshakeException

class PostgreSQLSSLConnectionSpec : DatabaseTestHelper() {

    @Test
    fun `ssl handler should connect to the database in ssl without verifying CA`() {
        withSSLHandler("127.0.0.1", defaultSslConfig.copy(rootCert = null)) { handler ->
            assertThat(handler.isReadyForQuery()).isTrue()
        }
    }

    @Test
    fun `ssl handler should connect to the database in ssl verifying CA`() {
        withSSLHandler("127.0.0.1", defaultSslConfig.copy(mode = SSLConfiguration.Mode.VerifyCA)) { handler ->
            assertThat(handler.isReadyForQuery()).isTrue()
        }
    }

    @Test
    fun `ssl handler should connect to the database in ssl verifying CA and hostname`() {
        withSSLHandler(sslConfig = defaultSslConfig.copy(mode = SSLConfiguration.Mode.VerifyFull)) { handler ->
            assertThat(handler.isReadyForQuery()).isTrue()
        }
    }

    @Test
    fun `ssl handler should throws exception when CA verification fails`() {
        verifyException(ExecutionException::class.java, SSLHandshakeException::class.java) {
            withSSLHandler(sslConfig = SSLConfiguration(SSLConfiguration.Mode.VerifyCA, rootCert = null)) {
            }
        }
    }

    @Test
    fun `ssl handler should throws exception when hostname verification fails`() {
        verifyException(ExecutionException::class.java, SSLHandshakeException::class.java) {
            withSSLHandler("127.0.0.1", defaultSslConfig.copy(SSLConfiguration.Mode.VerifyFull)) {
            }
        }
    }

    @Test
    fun `ssl handler should connect with a local client cert`() {
        val clientSsl = SelfSignedCertificate()
        val config = defaultSslConfig.copy(
            clientCert = clientSsl.certificate(),
            clientPrivateKey = clientSsl.privateKey()
        )
        withSSLHandler(sslConfig = config) { handler ->
            assertThat(handler.isReadyForQuery()).isTrue()
        }
    }
}
