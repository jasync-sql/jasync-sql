package oshai;

import com.github.jasync.r2dbc.mysql.JasyncConnectionFactory;
import com.github.jasync.sql.db.Configuration;
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.function.DatabaseClient;
import org.springframework.data.r2dbc.function.DefaultReactiveDataAccessStrategy;

public class Main {
    public static void main(String[] args) {

        MySQLConnectionFactory mycf = new MySQLConnectionFactory(
                new Configuration("test", "localhost", 3306, "123456", "test"));

        JasyncConnectionFactory cf = new JasyncConnectionFactory(mycf);

        DatabaseClient client = DatabaseClient.builder().connectionFactory(cf)
                .dataAccessStrategy(new DefaultReactiveDataAccessStrategy(PostgresDialect.INSTANCE)).build();

        client.execute()
                .sql("SELECT * FROM user")
                .map((row, rowMetadata) -> row.get("firstname"))
                .all()
                .doOnNext(it -> {
                    System.out.println("Record: " + it);
                })
                .blockFirst();
//                .as(StepVerifier::create)
//                .expectNextCount(585)
//                .verifyComplete();

    }
}
