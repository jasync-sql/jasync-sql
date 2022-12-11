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

    fun enqueue(dbExecutionTask: DbExecutionTask) {
        try {
            dbExecutionTaskQueue.offer(dbExecutionTask)
        } catch (e: IllegalStateException) {
            // queue capacity full
            logger.error("Failed to enqueue DbExecutionTask $e", e)
            dbExecutionTask.sink.error(e)
        }
    }

    override fun run() {
        while (true) {
            if (dbExecutionTaskQueue.isNotEmpty()) {
                val task = dbExecutionTaskQueue.poll()
                try {
                    val result = jasyncConnection.sendQuery(task.sql).join()
                    task.sink.success(result)
                } catch (e: Exception) {
                    logger.error("Exception on sendQuery - $e", e)
                    task.sink.error(e)
                }
            }
        }
    }
}
