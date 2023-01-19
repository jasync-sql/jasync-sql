package com.github.jasync.sql.db

import org.assertj.core.api.Assertions.assertThat
import kotlin.test.Test

class ConfigurationTest {

    @Test
    fun `test password is not in toDebugString`() {
        val string = Configuration(username = "myuser", password = "pass").toString()
        assertThat(string).contains(", password=****, database=")
    }

    @Test
    fun `test toDebugString should find only password - non greedy`() {
        val string = Configuration(username = "myuser", password = "mypass", database = ", password=myNotPass, database=bla").toString()
        assertThat(string).contains(", password=****, database=")
        assertThat(string).doesNotContain("mypass")
        assertThat(string).contains("myNotPass")
    }
}
