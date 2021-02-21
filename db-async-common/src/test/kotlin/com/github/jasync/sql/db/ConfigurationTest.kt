package com.github.jasync.sql.db

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class ConfigurationTest {

    @Test
    fun `test password is not in toDebugString`() {
        val string = Configuration(username = "myuser", password = "pass").toDebugString()
        assertThat(string).contains(", password=****, database=")
    }
}
