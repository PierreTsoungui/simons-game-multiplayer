package com.example.player;
import com.example.game.GameStateManager;

import com.example.database.DatabaseClient;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.Tuple;
import org.mindrot.jbcrypt.BCrypt;

public class PlayerRepository {
    private final JDBCPool jdbcPool;

    PlayerRepository(){
        this.jdbcPool = DatabaseClient.getInstance();
    }

    void insertPlayer(String playerName, String hashpassword,/*String controllerId, */Handler<AsyncResult<Void>> resultHandler) {

        //Execute the insert query without preparing metadata
        String query = "INSERT INTO players (playerName, password_hash)  VALUES (?, ?)";
        jdbcPool.preparedQuery(query)
                .execute(Tuple.of(playerName, hashpassword/* , controllerId*/), ar -> {
                    if (ar.succeeded()) {
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }

    /*void checkPlayerInDB(String playerName, String password, String controllerId, Handler<AsyncResult<RowSet<io.vertx.sqlclient.Row>>> resultHandler, GameStateManager gsm) {
        //Execute the select query without preparing metadata
        String query = "SELECT playerId, hashPassword FROM players WHERE playerName = ?";
        jdbcPool.preparedQuery(query)
                .execute(Tuple.of(playerName), ar -> {
                    if (ar.succeeded()) {
                        RowSet<io.vertx.sqlclient.Row> rows = ar.result();// Check if the player exists
                        if (rows.rowCount() == 0) {
                            resultHandler.handle(Future.failedFuture("No player found with the given name!"));
                            return;
                        }
                        io.vertx.sqlclient.Row row = rows.iterator().next();
                        String hashedPassword = row.getString("hashPassword");
                        int playerId = row.getInteger("playerId");


                        if(BCrypt.checkpw(password, hashedPassword)) {

                            if(!gsm.addPlayerToController(String.valueOf(playerId), controllerId)) {
                                resultHandler.handle(Future.failedFuture("Unavaiable controller!"));
                            }

                        } else {
                            resultHandler.handle(Future.failedFuture("Incorrect password!"));
                        }

                        resultHandler.handle(Future.succeededFuture(ar.result()));
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });
    }*/

    void checkPlayerInDB(String playerName, String password, String controllerId,
                         Handler<AsyncResult<RowSet<io.vertx.sqlclient.Row>>> resultHandler,
                         GameStateManager gsm) {

        String query = "SELECT playerId, password_hash FROM players WHERE playerName = ?";

        jdbcPool.preparedQuery(query)
                .execute(Tuple.of(playerName), ar -> {
                    if (ar.failed()) {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                        return;
                    }

                    RowSet<io.vertx.sqlclient.Row> rows = ar.result();

                    if (rows.rowCount() == 0) {
                        resultHandler.handle(Future.failedFuture("No player found with the given name!"));
                        return;
                    }

                    io.vertx.sqlclient.Row row = rows.iterator().next();
                    String hashedPassword = row.getString("password_hash");
                    int playerId = row.getInteger("playerId");

                    if (!BCrypt.checkpw(password, hashedPassword)) {
                        resultHandler.handle(Future.failedFuture("Incorrect password!"));
                        return;
                    }

                    boolean added = gsm.addPlayerToController(String.valueOf(playerId), controllerId);
                    if (!added) {
                        resultHandler.handle(Future.failedFuture("Unavailable controller!"));
                        return;
                    }

                    // success case – all checks passed
                    resultHandler.handle(Future.succeededFuture(rows));
                });
    }


}
