package com.github.jasync.sql.db.mysql

import com.github.jasync.sql.db.RowData
import mu.KotlinLogging
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}


/**
 * Mainly a way to try to figure out why sometimes MySQL will fail with a bad prepared statement response message.
 */
object ConcurrentlyRunTest : ConnectionHelper(), Runnable {


    @JvmStatic
    fun main(args: Array<String>) {

        logger.info("Starting executing code")

        val threads = 1.until(10).map { x -> Thread(this) }

        threads.forEach { t -> t.start() }

        while (threads.any { it.isAlive }) {
            Thread.sleep(5000)
        }

        logger.info("Finished executing code, failed execution ${ConcurrentlyRunTest.failures.get()} times")


    }

    private val counter = AtomicInteger()
    private val failures = AtomicInteger()

    override fun run() {
        1.until(50).forEach { x -> execute(counter.incrementAndGet()) }
    }


    private fun execute(count: Int) {
        try {
            logger.info("====> run $count")
            val create = """CREATE TEMPORARY TABLE posts (
                     |       id INT NOT NULL AUTO_INCREMENT,
                     |       some_text TEXT not null,
                     |       some_date DATE,
                     |       primary key (id) )""".trimMargin("|")

            val insert = "insert into posts (some_text) values (?)"
            val select = "select * from posts limit 100"

            withConnection { connection ->
                executeQuery(connection, create)

                executePreparedStatement(connection, insert, listOf("this is some text here"))

                val row: RowData = executeQuery(connection, select).rows!!.get(0)
                assert(row["id"] == 1)
                assert(row["some_text"] == "this is some text here")
                assert(row["some_date"] == null)

                val queryRow = executePreparedStatement(connection, select).rows!!.get(0)

                assert(queryRow["id"] == 1)
                assert(queryRow["some_text"] == "this is some text here")
                assert(queryRow["some_date"] == null)

                logger.info("====> run $count end")

            }
        } catch (e: Exception) {
            failures.incrementAndGet()
            logger.error("Failed to execute on run $count - ${e.message}", e)
        }
    }

}


