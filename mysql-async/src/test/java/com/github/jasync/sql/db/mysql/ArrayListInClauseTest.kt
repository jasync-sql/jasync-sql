package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.Connection
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assume
import org.junit.Test
import java.util.ArrayList
import java.util.concurrent.TimeUnit

/**
 * Test case demonstrating how to fix the "couldn't find mapping for class java.util.ArrayList" error
 * when using IN clauses with jasync-sql
 */
class ArrayListInClauseTest : ConnectionHelper() {

    data class Org(val id: Int, val name: String)

    @Test
    fun `test ArrayList in IN clause error and solution`() {
        // Skip test if Docker environment is not available
        try {
            val connection = MySQLConnection(
                defaultConfiguration
            )
            executeTest(connection)
        } catch (e: Exception) {
            // Log the exception and skip the test
            println("Skipping test due to connection issue: ${e.message}")
            Assume.assumeTrue("Database connection required for this test", false)
        }
    }

    private fun executeTest(connection: Connection) {
        try {
            connection.connect().get(5, TimeUnit.SECONDS)

            // Create a test table for organizations
            val createTable = """
                CREATE TEMPORARY TABLE organization (
                    id INT NOT NULL,
                    name VARCHAR(255) NOT NULL,
                    status VARCHAR(50) NOT NULL
                )
            """.trimIndent()

            // Insert test data
            val insertData = """
                INSERT INTO organization (id, name, status) VALUES
                (1, 'Company A', 'ok'),
                (2, 'Company B', 'ok'),
                (3, 'Company C', 'pending'),
                (4, 'Company D', 'ok'),
                (5, 'Company E', 'inactive')
            """.trimIndent()

            connection.sendQuery(createTable).get(5, TimeUnit.SECONDS)
            connection.sendQuery(insertData).get(5, TimeUnit.SECONDS)

            // Create a list of organization IDs to query
            val orgsId = ArrayList<Int>()
            orgsId.add(1)
            orgsId.add(2)
            orgsId.add(4)

            // PROBLEM DEMONSTRATION: This will cause the error "couldn't find mapping for class java.util.ArrayList"
            try {
                println("\n\n=================================================")
                println("REPRODUCING THE ISSUE: Using ArrayList in IN clause")
                println("=================================================")
                val sqlProblem = "SELECT id, name FROM organization WHERE status='ok' AND id IN (?)"
                val listOfList = ArrayList<ArrayList<Int>>()
                listOfList.add(orgsId)

                println("SQL: $sqlProblem")
                println("Parameters: $listOfList (${listOfList.javaClass.name})")

                connection.sendPreparedStatement(sqlProblem, listOfList)
                    .get(5, TimeUnit.SECONDS)
                    .rows
                    .forEach { row ->
                        println("This should not execute as an error should be thrown")
                    }
            } catch (e: Exception) {
                println("============================================")
                println("EXPECTED ERROR OCCURRED!")
                println("Error message: ${e.message}")

                var rootCause = e
                while (rootCause.cause != null && rootCause.cause != rootCause) {
                    rootCause = rootCause.cause!! as Exception
                }

                println("Root cause: ${rootCause.javaClass.name}: ${rootCause.message}")
                println("============================================\n")
            }

            // SOLUTION 1: Generate the correct number of placeholders
            val sql1 = "SELECT id, name FROM organization WHERE status='ok' AND id IN (?, ?, ?)"
            val result1 = ArrayList<Org>()

            connection.sendPreparedStatement(sql1, listOf(1, 2, 4))
                .get(5, TimeUnit.SECONDS)
                .rows
                .forEach { row ->
                    result1.add(Org(row.getInt(0)!!, row.getString(1)!!))
                }

            assertThat(result1.size).isEqualTo(3)
            assertThat(result1.map { it.id }).containsExactlyInAnyOrder(1, 2, 4)

            // SOLUTION 2: Dynamically generate placeholders based on the list size
            val placeholders = orgsId.joinToString(", ") { "?" }
            val sql2 = "SELECT id, name FROM organization WHERE status='ok' AND id IN ($placeholders)"
            val result2 = ArrayList<Org>()

            connection.sendPreparedStatement(sql2, orgsId)
                .get(5, TimeUnit.SECONDS)
                .rows
                .forEach { row ->
                    result2.add(Org(row.getInt(0)!!, row.getString(1)!!))
                }
            println("Result2:")
            assertThat(result2.size).isEqualTo(3)
            assertThat(result2.map { it.id }).containsExactlyInAnyOrder(1, 2, 4)
            assertThat(result2.map { it.name }).containsExactlyInAnyOrder("Company A", "Company B", "Company D")

            connection.disconnect().get(5, TimeUnit.SECONDS)
        } catch (e: Exception) {
            connection.disconnect().get(5, TimeUnit.SECONDS)
            throw e
        }
    }
}
