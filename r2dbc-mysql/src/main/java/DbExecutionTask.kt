import com.github.jasync.sql.db.QueryResult
import com.github.jasync.sql.db.interceptor.PreparedStatementParams
import reactor.core.publisher.MonoSink

interface DbExecutionTask {
    val sink: MonoSink<QueryResult>
}

data class QueryExecutionTask(
    override val sink: MonoSink<QueryResult>,
    val sql: String
) : DbExecutionTask {
    override fun toString(): String {
        return this.sql
    }
}

data class PreparedStatementExecutionTask(
    override val sink: MonoSink<QueryResult>,
    val preparedStatementParams: PreparedStatementParams
) : DbExecutionTask {
    override fun toString(): String {
        return this.preparedStatementParams.toString()
    }
}
