package com.example.highscore;

import com.example.database.DatabaseClient;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.RowSet;

public class HighscoreRepository {

    private final JDBCPool jdbcPool;

    HighscoreRepository() {
        this.jdbcPool = DatabaseClient.getInstance();
    }

    void fetchHighscoreData(Handler<AsyncResult<RowSet<io.vertx.sqlclient.Row>>> resultHandler) {
        String query = "SELECT p.playerName, hs.score, hs.duration " +
                "FROM highScore hs " +
                "JOIN players p ON hs.playerId = p.playerId " +
                "ORDER BY hs.score DESC, hs.duration ASC ";

        //We use preparedQuery even without params, good practice (against SQL injection)
        jdbcPool.preparedQuery(query)
                .execute(ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture(ar.result()));
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }
}