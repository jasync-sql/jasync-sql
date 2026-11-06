package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.SSLConfiguration.Mode
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.util.Base64
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import kotlin.concurrent.thread

/**
 * Tests the authentication phase against a minimal in-process MySQL server, so they run without Docker and can
 * script the exact packets a real server sends (see AuthenticationSpec for the same flows against real servers).
 *
 * The interesting case is a server whose default plugin differs from the plugin of the account: the initial
 * handshake advertises one plugin, then the server answers the handshake response with an authentication switch
 * request naming the plugin of the account. Everything that follows must be interpreted against the plugin (and
 * the seed) from the switch request, not the ones from the initial handshake.
 */
class AuthenticationSwitchSpec {

    private lateinit var keyPair: KeyPair
    private lateinit var publicKeyFile: Path
    private var server: FakeServer? = null
    private var connection: MySQLConnection? = null

    @Before
    fun setUp() {
        keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        publicKeyFile = Files.createTempFile("jasync-auth-switch", ".pem")
        val base64 = Base64.getMimeEncoder(64, "\n".toByteArray()).encodeToString(keyPair.public.encoded)
        Files.write(publicKeyFile, "-----BEGIN PUBLIC KEY-----\n$base64\n-----END PUBLIC KEY-----\n".toByteArray())
    }

    @After
    fun tearDown() {
        // The fake server closes the socket once its script is done, so there is nothing to wait for.
        connection?.disconnect()
        server?.close()
        Files.deleteIfExists(publicKeyFile)
    }

    @Test
    fun `full authentication after a switch from a native handshake scrambles the password with the seed of the switch request`() {
        val handshakeSeed = seed(1)
        val switchSeed = seed(2)
        val fake = startServer { io ->
            io.write(0, handshake(NATIVE, handshakeSeed))
            io.read() // handshake response
            io.write(2, authSwitchRequest(CACHING_SHA2, switchSeed))
            io.read() // scrambled password
            io.write(4, authMoreData(PERFORM_FULL_AUTHENTICATION))
            io.read() // password encrypted with the public key
            io.write(6, OK)
        }

        connect(fake, Mode.Disable, rsaPublicKey = publicKeyFile).get(10, TimeUnit.SECONDS)

        assertThat(fake.received).hasSize(3)
        assertThat(decryptPassword(fake.received[2], switchSeed)).isEqualTo("secret")
        assertThat(decryptPassword(fake.received[2], handshakeSeed)).isNotEqualTo("secret")
    }

    @Test
    fun `fast authentication after a switch from a native handshake succeeds without sending anything else`() {
        val fake = startServer { io ->
            io.write(0, handshake(NATIVE, seed(1)))
            io.read() // handshake response
            io.write(2, authSwitchRequest(CACHING_SHA2, seed(2)))
            io.read() // scrambled password
            io.write(4, authMoreData(FAST_AUTHENTICATION_SUCCESS))
            io.write(5, OK)
        }

        connect(fake, Mode.Disable).get(10, TimeUnit.SECONDS)

        // Handshake response and the answer to the switch request, no full authentication.
        assertThat(fake.received).hasSize(2)
    }

    @Test
    fun `full authentication without a switch scrambles the password with the seed of the initial handshake`() {
        val handshakeSeed = seed(1)
        val fake = startServer { io ->
            io.write(0, handshake(CACHING_SHA2, handshakeSeed))
            io.read() // handshake response
            io.write(2, authMoreData(PERFORM_FULL_AUTHENTICATION))
            io.read() // password encrypted with the public key
            io.write(4, OK)
        }

        connect(fake, Mode.Disable, rsaPublicKey = publicKeyFile).get(10, TimeUnit.SECONDS)

        assertThat(fake.received).hasSize(2)
        assertThat(decryptPassword(fake.received[1], handshakeSeed)).isEqualTo("secret")
    }

    @Test
    fun `full authentication after a switch from a native handshake without ssl or an rsa key fails with the unsafe connection error`() {
        val fake = startServer { io ->
            io.write(0, handshake(NATIVE, seed(1)))
            io.read() // handshake response
            io.write(2, authSwitchRequest(CACHING_SHA2, seed(2)))
            io.read() // scrambled password
            io.write(4, authMoreData(PERFORM_FULL_AUTHENTICATION))
        }

        assertThatThrownBy {
            connect(fake, Mode.Disable).get(10, TimeUnit.SECONDS)
        }.hasRootCauseInstanceOf(IllegalStateException::class.java)
            .hasRootCauseMessage("Authentication is not possible over an unsafe connection. Please use SSL or specify 'rsaPublicKey'")
    }

    @Test
    fun `a switch to native password completes without further packets`() {
        val fake = startServer { io ->
            io.write(0, handshake(CACHING_SHA2, seed(1)))
            io.read() // handshake response
            io.write(2, authSwitchRequest(NATIVE, seed(2)))
            io.read() // scrambled password
            io.write(4, OK)
        }

        connect(fake, Mode.Disable).get(10, TimeUnit.SECONDS)

        assertThat(fake.received).hasSize(2)
    }

    @Test
    fun `an AuthMoreData packet after a switch away from caching_sha2_password is rejected`() {
        // The initial handshake advertises caching_sha2_password but the server then switches to
        // mysql_native_password, which never sends AuthMoreData: the packet is not valid for the plugin in use.
        val fake = startServer { io ->
            io.write(0, handshake(CACHING_SHA2, seed(1)))
            io.read() // handshake response
            io.write(2, authSwitchRequest(NATIVE, seed(2)))
            io.read() // scrambled password
            io.write(4, authMoreData(PERFORM_FULL_AUTHENTICATION))
        }

        assertThatThrownBy {
            connect(fake, Mode.Disable, rsaPublicKey = publicKeyFile).get(10, TimeUnit.SECONDS)
        }.hasRootCauseInstanceOf(IllegalStateException::class.java)
            .hasRootCauseMessage("AuthMoreDataMessage is only supported for 'caching_sha2_password' method")
    }

    private fun connect(
        fake: FakeServer,
        sslMode: Mode,
        rsaPublicKey: Path? = null
    ): CompletableFuture<MySQLConnection> {
        val configuration = Configuration(
            username = "user",
            password = "secret",
            host = "127.0.0.1",
            port = fake.port,
            ssl = SSLConfiguration(sslMode),
            rsaPublicKey = rsaPublicKey
        )
        val newConnection = MySQLConnection(configuration)
        connection = newConnection
        return newConnection.connect()
    }

    private fun startServer(script: (PacketIo) -> Unit): FakeServer {
        return FakeServer(script).also { server = it }
    }

    /** Decrypts the full authentication packet and reverts the XOR with [seed]. */
    private fun decryptPassword(packet: ByteArray, seed: ByteArray): String {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
        cipher.init(Cipher.DECRYPT_MODE, keyPair.private)
        val scrambled = cipher.doFinal(packet)
        val password = ByteArray(scrambled.size) { index -> (scrambled[index].toInt() xor seed[index % seed.size].toInt()).toByte() }
        // The password is sent with a trailing NUL.
        return String(password.copyOf(password.size - 1))
    }

    private fun seed(base: Int): ByteArray = ByteArray(20) { index -> (base * 31 + index * 7 + 1).toByte() }

    private fun handshake(plugin: String, seed: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(10) // protocol version
        out.write("8.0.32".toByteArray())
        out.write(0)
        out.write(byteArrayOf(1, 0, 0, 0)) // connection id
        out.write(seed, 0, 8)
        out.write(0) // filler
        out.write(byteArrayOf(0x0F, 0xA2.toByte())) // capabilities (lower), CLIENT_SSL is not offered
        out.write(33) // character set
        out.write(byteArrayOf(2, 0)) // status flags: autocommit
        out.write(byteArrayOf(0x0A, 0)) // capabilities (upper): CLIENT_MULTI_RESULTS | CLIENT_PLUGIN_AUTH
        out.write(21) // length of auth plugin data
        out.write(ByteArray(10)) // reserved
        out.write(seed, 8, 12)
        out.write(0)
        out.write(plugin.toByteArray())
        out.write(0)
        return out.toByteArray()
    }

    private fun authSwitchRequest(plugin: String, seed: ByteArray): ByteArray {
        val out = ByteArrayOutputStream()
        out.write(0xFE)
        out.write(plugin.toByteArray())
        out.write(0)
        out.write(seed)
        out.write(0)
        return out.toByteArray()
    }

    private fun authMoreData(data: Int): ByteArray = byteArrayOf(0x01, data.toByte())

    /** Reads and writes MySQL packets: 3 bytes length, 1 byte sequence id, payload. */
    private class PacketIo(input: InputStream, private val output: OutputStream) {
        private val input = DataInputStream(input)
        val received = CopyOnWriteArrayList<ByteArray>()

        fun read(): ByteArray {
            val header = ByteArray(4)
            input.readFully(header)
            val length = (header[0].toInt() and 0xff) or
                ((header[1].toInt() and 0xff) shl 8) or
                ((header[2].toInt() and 0xff) shl 16)
            val payload = ByteArray(length)
            input.readFully(payload)
            received.add(payload)
            return payload
        }

        fun write(sequenceId: Int, payload: ByteArray) {
            output.write(payload.size and 0xff)
            output.write((payload.size shr 8) and 0xff)
            output.write((payload.size shr 16) and 0xff)
            output.write(sequenceId)
            output.write(payload)
            output.flush()
        }
    }

    /** Accepts one connection, runs the script and records the packets received from the client. */
    private class FakeServer(script: (PacketIo) -> Unit) : AutoCloseable {
        private val serverSocket = ServerSocket(0, 1, InetAddress.getByName("127.0.0.1"))
        private val io = CompletableFuture<PacketIo>()
        val port: Int = serverSocket.localPort
        val received: List<ByteArray> get() = io.get(10, TimeUnit.SECONDS).received

        init {
            thread(isDaemon = true, name = "fake-mysql-server") {
                try {
                    serverSocket.accept().use { socket ->
                        socket.soTimeout = 10_000
                        val packetIo = PacketIo(socket.getInputStream(), socket.getOutputStream())
                        io.complete(packetIo)
                        script(packetIo)
                    }
                } catch (e: Exception) {
                    io.completeExceptionally(e)
                }
            }
        }

        override fun close() {
            serverSocket.close()
        }
    }

    private companion object {
        const val NATIVE = "mysql_native_password"
        const val CACHING_SHA2 = "caching_sha2_password"
        const val FAST_AUTHENTICATION_SUCCESS = 3
        const val PERFORM_FULL_AUTHENTICATION = 4
        val OK = byteArrayOf(0, 0, 0, 2, 0, 0, 0)
    }
}
