package com.github.jasync.sql.db

import kotlin.test.Test
import org.assertj.core.api.Assertions.assertThat

class ConfigurationTest {

    @Test
    fun `test password is not in toDebugString`() {
        val string = Configuration(username = "myuser", password = "pass").toDebugString()
        assertThat(string).contains(", password=****, database=")
    }

    @Test
    fun `test toDebugString should find only password - non greedy`() {
        val string = Configuration(username = "myuser", password = "mypass", database = ", password=myNotPass, database=bla").toDebugString()
        assertThat(string).contains(", password=****, database=")
        assertThat(string).doesNotContain("mypass")
        assertThat(string).contains("myNotPass")
    }
}
