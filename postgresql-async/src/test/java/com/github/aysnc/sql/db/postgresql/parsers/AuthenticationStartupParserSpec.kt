package com.github.aysnc.sql.db.postgresql.parsers

import com.github.jasync.sql.db.postgresql.parsers.AuthenticationStartupParser
import com.github.jasync.sql.db.util.length
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.nio.charset.StandardCharsets

class AuthenticationStartupParserSpec {

    @Test
    fun `SASSL mechanisms should be parsed if there is only one mechanism`() {
        assertSASLMechanismParsingRoundTrip(listOf("SCRAM-SHA-256"))
    }

    @Test
    fun `SASSL mechanisms should be parsed if there are many mechanisms`() {
        assertSASLMechanismParsingRoundTrip(listOf("SCRAM-SHA-256", "SCRAM-SHA-256-PLUS", "mechanism 3"))
    }

    private fun assertSASLMechanismParsingRoundTrip(mechanisms: List<String>) {
        assertThat(AuthenticationStartupParser.parseSASLMechanismIds(encodeMechanisms(mechanisms)))
            .isEqualTo(mechanisms)
    }

    private fun encodeMechanisms(mechanisms: List<String>) = mechanisms
        .map { it.toByteArray(StandardCharsets.UTF_8) }
        .fold(ByteArray(0)) { existing, new ->
            // +1 to add null byte after every string to terminate the string
            ByteArray(existing.length + new.length + 1).also {
                existing.copyInto(it)
                new.copyInto(it, existing.length)
            }
        }
        .let { r -> ByteArray(r.length + 1).also { r.copyInto(it) } } // Add null byte at the end of message, per spec
}
