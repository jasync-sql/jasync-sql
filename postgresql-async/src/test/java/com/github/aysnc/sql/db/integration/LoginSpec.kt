package com.github.aysnc.sql.db.integration

import com.github.aysnc.sql.db.verifyException
import com.github.jasync.sql.db.exceptions.UnsupportedAuthenticationMethodException
import com.github.jasync.sql.db.invoke
import com.github.jasync.sql.db.postgresql.exceptions.GenericDatabaseException
import java.util.concurrent.ExecutionException
import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.junit.Ignore
import org.junit.Test

class LoginSpec : DatabaseTestHelper() {
    @Test
    fun `"handler" should     "login using MD5 authentication"`() {

        val configuration = conf.copy(
            username = "postgres_md5",
            password = "postgres_md5"
        )

        withHandler(configuration) { handler ->
            val result = executeQuery(handler, "SELECT 0")
            Assertions.assertThat(result.rows(0)(0)).isEqualTo(0)
        }
    }

    @Test
    fun `"handler" should     "login using cleartext authentication"`() {

        val configuration = conf.copy(
            username = "postgres_cleartext",
            password = ("postgres_cleartext")
        )

        withHandler(configuration) { handler ->
            val result = executeQuery(handler, "SELECT 0")
            Assertions.assertThat(result.rows.get(0)(0)).isEqualTo(0)
        }
    }

    @Ignore("docker image does not support kerberos, this is used to cover AuthenticationStartupParser")
    @Test
    fun `"handler" should     "fail login using kerberos authentication"`() {

        val configuration = conf.copy(
            username = "postgres_kerberos",
            password = ("postgres_kerberos")

        )

        verifyException(ExecutionException::class.java, UnsupportedAuthenticationMethodException::class.java) {
            withHandler(configuration) { handler ->
                executeQuery(handler, "SELECT 0")
            }
        }
    }

    @Test
    fun `"handler" should     "fail login using , an invalid credential exception"`() {

        val configuration = conf.copy(
            username = "postgres_md5",
            password = ("postgres_kerberos")
        )
        val e: GenericDatabaseException =
            verifyException(ExecutionException::class.java, GenericDatabaseException::class.java) {
                withHandler(configuration) { handler ->
                    executeQuery(handler, "SELECT 0")
                }
            } as GenericDatabaseException
        assertThat(e.errorMessage.fields['R']).isEqualTo("auth_failed")
    }
}
