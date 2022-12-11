import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import com.github.jasync.sql.db.Connection as JasyncConnection

class QueueingExecutionJasyncConnectionAdapter(
    private val delegate: JasyncConnection,
    private val queueingQueryExecutor: QueueingQueryExecutor
) : Connection by delegate {
    override fun sendQuery(query: String): CompletableFuture<QueryResult> {
        return Mono.create<QueryResult> { queueingQueryExecutor.enqueue(DbExecutionTask(it, query)) }
            .toFuture()
    }

    override fun disconnect(): CompletableFuture<Connection> {
        queueingQueryExecutor.shutdown()
        return delegate.disconnect()
    }
}
