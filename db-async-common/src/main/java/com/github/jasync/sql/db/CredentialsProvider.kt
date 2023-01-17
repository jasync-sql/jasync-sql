package com.github.jasync.sql.db

import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

/**
 * A CredentialsProvider that can be used to provide the connection configuration
 * with on demand credentials on connect.
 * It can be used to implement time based credential authentication,
 * eg. IAM based authentication for AWS RDS or Hashicorp Vault database secrets.
 */
interface CredentialsProvider {
    fun provide(): CompletionStage<Credentials>
}

data class Credentials(val username: String, val password: String?) {
    override fun toString(): String = "Credentials (username: $username, password: *****)"
}

/**
 * A StaticCredentialsProvider implements the CredentialsProvider interface with
 * a static username / password combination.
 * It exists mainly to provide a simplified interface for static credentials configuration
 * that can be set on the connection configuration directly.
 * It is rarely necessary to use this class directly (outside of the driver code base).
 */
data class StaticCredentialsProvider(val username: String, val password: String?) : CredentialsProvider {

    override fun provide(): CompletionStage<Credentials> =
        CompletableFuture.completedFuture(Credentials(username, password))

    override fun toString(): String {
        return "StaticCredentialsProvider(username='$username', password=redacted)"
    }
}
