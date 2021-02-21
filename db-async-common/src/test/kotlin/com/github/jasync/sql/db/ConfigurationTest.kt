package com.github.jasync.sql.db

import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat

class ConfigurationTest {

    @Test
    fun `test password is not in toDebugString`() {
        val string = Configuration(username = "myuser", password = "pass").toDebugString()
        assertThat(string).contains(", password=****, database=")
    }
}
