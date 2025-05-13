package com.example.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.PoolOptions;

public class DatabaseClient {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseClient.class);
    private static JDBCPool jdbcPool;

    private DatabaseClient() { // Singelton
    }

    public static void initialize(Vertx vertx, JsonObject config) {
        if (jdbcPool != null) {
            logger.warn("DatabaseClient is already initialized!");
            return;
        }

        try {

            logger.info("🔍 Checking Config: {}", config.encodePrettily());

            // 🔹 Validate Config Fields
            validateConfig(config);

            // 🔹 Create JDBC Pool
            jdbcPool = JDBCPool.pool(vertx,
                    new io.vertx.jdbcclient.JDBCConnectOptions()
                            .setJdbcUrl("jdbc:mariadb://" + config.getString("DB_HOST") + ":"
                                    + config.getInteger("DB_PORT") + "/" + config.getString("DB_NAME"))
                            .setUser(config.getString("DB_USER"))
                            .setPassword(config.getString("DB_PASSWORD")),
                    new PoolOptions().setMaxSize(5)
            );

            logger.info("✅ Database connection pool initialized successfully.");

            // 🔹 Test Database Connection
            testDatabaseConnection();
        } catch (Exception e) {
            logger.error("❌ Failed to initialize DatabaseClient: {}", e.getMessage());
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static JDBCPool getInstance() {
        if (jdbcPool == null) {
            throw new IllegalStateException("❌ DatabaseClient not initialized! Call initialize() first.");
        }
        return jdbcPool;
    }

    private static void validateConfig(JsonObject config) {
        logger.info("🔍 Checking Config: {}", config.encodePrettily());

        if (!config.containsKey("DB_HOST") || !config.containsKey("DB_PORT")
                || !config.containsKey("DB_NAME") || !config.containsKey("DB_USER")
                || !config.containsKey("DB_PASSWORD")) {
            throw new IllegalArgumentException("❌ Missing required database configuration parameters!");
        }
    }

    private static void testDatabaseConnection() {
        jdbcPool.query("SELECT 1").execute()
                .onSuccess(rows -> logger.info("✅ Database connection test successful!"))
                .onFailure(err -> logger.error("❌ Database connection test failed: {}", err.getMessage()));
    }
}
