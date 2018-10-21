package oshai;

import com.github.jasync.sql.db.Configuration;
import com.github.jasync.sql.db.Connection;
import com.github.jasync.sql.db.QueryResult;
import com.github.jasync.sql.db.pool.ConnectionPool;
import com.github.jasync.sql.db.pool.PoolConfiguration;
import com.github.jasync.sql.db.postgresql.pool.PostgreSQLConnectionFactory;
import io.javalin.Javalin;
import io.javalin.JavalinEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class Main {
  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    PoolConfiguration poolConfiguration = new PoolConfiguration(
      100,                            // maxObjects
      TimeUnit.MINUTES.toMillis(15),  // maxIdle
      10_000,                         // maxQueueSize
      TimeUnit.SECONDS.toMillis(30)   // validationInterval
    );
    Connection connection = new ConnectionPool<>(
      // for PostgreSQL use PostgreSQLConnectionFactory instead of MySQLConnectionFactory
      new PostgreSQLConnectionFactory(new Configuration(
        "postgres",
        "localhost",
        5432,
        "mysecretpassword",
        "postgres"
      )), poolConfiguration);
    Javalin app = Javalin.create()
      .event(JavalinEvent.SERVER_STARTING, () -> {
        logger.info("--- SERVER STARTING!");
        connection.connect().get();
        logger.info("--- connection STARTED!");
      })
      .event(JavalinEvent.SERVER_STOPPING, () -> {
        logger.info("--- SERVER STOPPING!");
        connection.disconnect().get();
        logger.info("--- connection STOPPED!");
      })
      .start(7000);

    app.get("/", (ctx) -> {
      final CompletableFuture<QueryResult> queryResultCompletableFuture = connection.sendPreparedStatement("select 0");
      ctx.result(
        queryResultCompletableFuture
          .thenApply((t) -> "got result: " + t.getRows().get(0).get(0))
      );
    });
  }
}
