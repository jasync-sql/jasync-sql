package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.SSLConfiguration
import org.assertj.core.api.Assertions
import org.junit.Test
import java.io.File
import java.util.concurrent.ExecutionException
import javax.net.ssl.SSLHandshakeException

class MySQLSSLConnectionSpec : ConnectionHelper() {

    private val defaultSslConfig = SSLConfiguration(
        SSLConfiguration.Mode.Require,
        resourceFile("server-cert.pem")
    )

    @Test
    fun `ssl handler should connect to the database in ssl without verifying CA`() {
        withSSLConnection("127.0.0.1", defaultSslConfig.copy(rootCert = null)) { handler ->
            Assertions.assertThat(handler.isConnected()).isTrue()
        }
    }

    @Test
    fun `ssl handler should connect to the database in ssl verifying CA`() {
        withSSLConnection("127.0.0.1", defaultSslConfig.copy(mode = SSLConfiguration.Mode.VerifyCA)) { handler ->
            Assertions.assertThat(handler.isConnected()).isTrue()
        }
    }

    @Test
    fun `ssl handler should connect to the database in ssl verifying CA and hostname`() {
        withSSLConnection(sslConfig = defaultSslConfig.copy(mode = SSLConfiguration.Mode.VerifyFull)) { handler ->
            Assertions.assertThat(handler.isConnected()).isTrue()
        }
    }

    @Test
    fun `ssl handler should throws exception when CA verification fails`() {
        verifyException(ExecutionException::class.java, SSLHandshakeException::class.java) {
            withSSLConnection(sslConfig = SSLConfiguration(SSLConfiguration.Mode.VerifyCA, rootCert = null)) {
            }
        }
    }

    @Test
    fun `ssl handler should throws exception when hostname verification fails`() {
        verifyException(ExecutionException::class.java, SSLHandshakeException::class.java) {
            withSSLConnection("127.0.0.1", defaultSslConfig.copy(SSLConfiguration.Mode.VerifyFull)) {
            }
        }
    }

    @Test
    fun `ssl handler should connect with a local client cert`() {
        val config = defaultSslConfig.copy(
            clientCert = resourceFile("client-cert.pem"),
            clientPrivateKey = resourceFile("client-key.pem")
        )
        withSSLConnection(sslConfig = config) { handler ->
            Assertions.assertThat(handler.isConnected()).isTrue()
        }
    }

    private fun resourceFile(name: String): File {
        return File(ClassLoader.getSystemClassLoader().getResource(name)!!.file)
    }
}
