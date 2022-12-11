import com.github.jasync.sql.db.QueryResult
import reactor.core.publisher.MonoSink

data class DbExecutionTask(
    val sink: MonoSink<QueryResult>,
    val sql: String
)
