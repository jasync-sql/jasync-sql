package me.pgs

import com.github.jasync.sql.db.Configuration
import com.github.jasync.sql.db.mysql.pool.MySQLConnectionFactory
import com.github.jasync.sql.db.pool.ConnectionPool
import com.github.jasync.sql.db.pool.PoolConfiguration
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit

@Component
class DB {
    private val configuration = Configuration(
            "test",
            "localhost",
            3306,
            "123456",
            "test")
    private val poolConfiguration = PoolConfiguration(
            maxObjects = 100,
            maxIdle = TimeUnit.MINUTES.toMillis(15),
            maxQueueSize = 10_000,
            validationInterval = TimeUnit.SECONDS.toMillis(30)
    )
    val connectionPool = ConnectionPool(factory = MySQLConnectionFactory(configuration), configuration = poolConfiguration)
}