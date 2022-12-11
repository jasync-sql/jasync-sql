import com.github.jasync.sql.db.Connection
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ArrayBlockingQueue

class QueueingQueryExecutor(
    private val jasyncConnection: Connection
) : Runnable {

    private val logger = LoggerFactory.getLogger(this::class.java)
    private val dbExecutionTaskQueue: Queue<DbExecutionTask> =
        ArrayBlockingQueue(256, true)

    @Volatile
    private var shutdown = false

    fun enqueue(dbExecutionTask: DbExecutionTask) {
        try {
            dbExecutionTaskQueue.offer(dbExecutionTask)
        } catch (e: IllegalStateException) {
            // queue capacity full
            logger.error("Failed to enqueue DbExecutionTask $e", e)
            dbExecutionTask.sink.error(e)
        }
    }

    fun shutdown() {
        this.shutdown = true
    }

    override fun run() {
        while (!shutdown) {
            if (dbExecutionTaskQueue.isNotEmpty()) {
                val task = dbExecutionTaskQueue.poll()
                try {
                    if (jasyncConnection.isConnected()) {
                        val result = jasyncConnection.sendQuery(task.sql).join()
                        task.sink.success(result)
                    } else {
                        logger.info("Dropping query because the connection has already been closed - ${task.sql}")
                        task.sink.error(IllegalStateException("Connection has been closed"))
                    }
                } catch (e: Exception) {
                    logger.error("Exception on sendQuery - $e", e)
                    task.sink.error(e)
                }
            }
        }
    }
}
