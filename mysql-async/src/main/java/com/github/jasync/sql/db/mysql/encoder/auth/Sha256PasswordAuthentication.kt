package com.github.jasync.sql.db.mysql.encoder.auth

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.SSLConfiguration
import com.github.jasync.sql.db.util.length
import mu.KotlinLogging
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.X509EncodedKeySpec
import java.util.Base64
import javax.crypto.Cipher
import kotlin.experimental.xor

private val logger = KotlinLogging.logger {}

object Sha256PasswordAuthentication : AuthenticationMethod {

    private val EmptyArray = ByteArray(0)
    private val RsaHeaderRegex = Regex("(-+BEGIN PUBLIC KEY-+|-+END PUBLIC KEY-+|\\r?\\n)")

    override fun generateAuthentication(charset: Charset, configuration: Configuration, seed: ByteArray): ByteArray {
        if (configuration.password == null) {
            return EmptyArray
        }

        val bytes = configuration.password!!.toByteArray(charset)
        val result = ByteArray(bytes.length + 1)
        bytes.copyInto(result)

        // We can send the plaintext password in SSL mode.
        if (configuration.ssl.mode != SSLConfiguration.Mode.Disable) {
            return result
        }

        // Otherwise we need to encrypt the password with an RSA public key.
        if (configuration.rsaPublicKey == null) {
            throw IllegalStateException(
                "Authentication is not possible over an unsafe connection. Please use SSL or specify 'rsaPublicKey'"
            )
        }

        for ((index, byte) in result.withIndex()) {
            result[index] = byte xor seed[index % seed.length]
        }

        val publicKey = try {
            getPublicKey(configuration.rsaPublicKey!!)
        } catch (e: Exception) {
            logger.error(e) { "Unable to read the RSA public key at '${configuration.rsaPublicKey}'" }
            throw e
        }

        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-1AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        return cipher.doFinal(result)
    }

    private fun getPublicKey(path: Path): PublicKey {
        val data = Files.readAllBytes(path).toString(Charsets.UTF_8).replace(RsaHeaderRegex, "")
        val bytes = Base64.getDecoder().decode(data)
        val keySpec = X509EncodedKeySpec(bytes)

        val factory = KeyFactory.getInstance("RSA")
        return factory.generatePublic(keySpec)
    }
}
