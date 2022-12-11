import com.github.jasync.sql.db.Connection
import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.interceptor.PreparedStatementParams
import reactor.core.publisher.Mono
import java.util.concurrent.CompletableFuture
import com.github.jasync.sql.db.Connection as JasyncConnection

class QueueingExecutionJasyncConnectionAdapter(
    private val delegate: JasyncConnection,
    private val queueingQueryExecutor: QueueingQueryExecutor
) : Connection by delegate {
    override fun sendQuery(query: String): CompletableFuture<QueryResult> {
        return Mono.create<QueryResult> { queueingQueryExecutor.enqueue(QueryExecutionTask(it, query)) }
            .toFuture()
    }

    override fun disconnect(): CompletableFuture<Connection> {
        queueingQueryExecutor.shutdown()
        return delegate.disconnect()
    }

    override fun sendPreparedStatement(
        query: String,
        values: List<Any?>,
        release: Boolean
    ): CompletableFuture<QueryResult> {
        return Mono.create<QueryResult> {
            queueingQueryExecutor.enqueue(
                PreparedStatementExecutionTask(
                    it,
                    PreparedStatementParams(query = query, values = values, release = release)
                )
            )
        }.toFuture()
    }
}
