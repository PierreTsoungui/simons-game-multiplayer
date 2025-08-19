package com.example.game;

import com.example.database.DatabaseClient;
import com.example.object.ObjectController;
import com.example.player.PlayerInfo;
import io.vertx.core.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.jdbcclient.JDBCPool;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.sqlclient.SqlConnection;
import io.vertx.sqlclient.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Future;

import java.sql.Time;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;


public class GameService {

    private static final Logger logger = LoggerFactory.getLogger(ObjectController.class);
    GameStateManager gsm;
    public static  volatile GameModel gameModel;
    private static GameService instance;
    private final JDBCPool jdbcPool;

    public GameService(Vertx vertx) {
        this.gsm=GameStateManager.getInstance(vertx);
        this.jdbcPool = DatabaseClient.getInstance();
    }



    public static synchronized GameService getInstance(Vertx vertx) {
        if (instance == null) {
            instance = new GameService(vertx);
        }
        return instance;
    }


    public void createGameInDB(String gameCode,Handler<AsyncResult<Void>> resultHandler) {
        String insertQuery = "INSERT INTO game ( gameId,isGameActive, startTime) VALUES (?, ?, ?)";

            jdbcPool.preparedQuery(insertQuery)
                    .execute(Tuple.of( gameCode ,true , java.time.LocalTime.now()), insertRes -> {
                        if (insertRes.failed()) {
                            resultHandler.handle(Future.failedFuture(insertRes.cause()));
                            return;
                        }

                        logger.info("Game created in database with gameId {}.", gameCode);
                        resultHandler.handle(Future.succeededFuture());
                    });
    }

    /**
     *
     *   wird verwenden ,um ein Spiel Zu Starten
     */

    public void startGame(Handler<AsyncResult<Void>> resultHandler) {
        String gameCode = UUID.randomUUID().toString();

        acquireGameLock(lockResult -> {
            if (lockResult.failed()) {
                resultHandler.handle(Future.failedFuture(lockResult.cause()));
                return;
            }
            //  Mindestens 2 Spielers als Ready markiert
            if (gsm.getReadyPlayers().isEmpty() || gsm.getReadyPlayers().size() == 1) {
                releaseGameLock();
                resultHandler.handle(Future.failedFuture("No players are ready to start the game."));
                return;
            }
            gsm.resetCurrentSequence();
            createGameInDB(gameCode, dbAr -> {
                if (dbAr.succeeded()) {
                    gameModel=  new GameModel(gsm.getReadyPlayers(), gameCode);
                    gsm.setCurrentSequence(gameModel.nextColors());

                    List<String> controllerIds = gsm.getReadyControllerForPlayer(gameModel.players);
                    JsonObject infoMessage = new JsonObject()
                            .put("info", "Spiel startet in 3 Sekunden...")
                            .put("controllerIds", new JsonArray(controllerIds));
                    gsm.getEventBus().publish("group-24.simon.game.info", infoMessage);

                    gsm.getVertx().setTimer(3000, id -> {
                        JsonObject startMessage = new JsonObject()
                                .put("round", gameModel.currentRound)
                                .put("sequence", new JsonArray(gsm.getCurrentSequenceAsString(gsm.getCurrentSequence())))
                                .put("inputTimeLimit",gsm.calculateInputTimeLimit(gsm.getCurrentSequence().size()))
                                .put("Point(s)",  "0")
                                .put("controllerIds", new JsonArray(controllerIds));
                        gsm.getEventBus().publish("group-24.simon.game.start", startMessage);
                        // Spiel läuft jetzt. Lock bleibt aktiv.
                    });

                    resultHandler.handle(Future.succeededFuture());

                } else {
                    releaseGameLock(); // Spiel konnte nicht erstellt werden → Lock freigeben
                    resultHandler.handle(Future.failedFuture(dbAr.cause()));
                }
            });
        });
    }


    private void acquireGameLock(Handler<AsyncResult<Void>> resultHandler) {
        String lockQuery = "UPDATE game_lock SET is_locked = TRUE WHERE id = 1 AND is_locked = FALSE";

        jdbcPool.preparedQuery(lockQuery).execute(lockAr -> {
            if (lockAr.succeeded() && lockAr.result().rowCount() == 1) {
                // Lock erfolgreich gesetzt
                resultHandler.handle(Future.succeededFuture());
            } else if (lockAr.succeeded()) {
                // Lock war bereits gesetzt
                resultHandler.handle(Future.failedFuture("A game is already running."));
            } else {
                // Datenbankfehler
                resultHandler.handle(Future.failedFuture(lockAr.cause()));
            }
        });
    }




    public Future<Void> releaseGameLock() {
        Promise<Void> promise = Promise.promise();
        String sql = "UPDATE game_lock SET is_locked = FALSE WHERE id = 1";
        jdbcPool.query(sql).execute(ar -> {
            if (ar.succeeded()) {
                promise.complete();
            } else {
                promise.fail(ar.cause());
            }
        });
        return promise.future();
    }


    private void endGameindb(String gameCode,Handler<AsyncResult<Void>> resultHandler) {
        String query = "UPDATE game SET isGameActive = ?, endTime = ? WHERE gameId= ?";
        jdbcPool.preparedQuery(query)
                .execute(Tuple.of(false, java.time.LocalTime.now(),  gameCode), ar -> {
                    if (ar.succeeded()) {
                        releaseGameLock();
                        logger.info("All active games terminated on startup.");
                        resultHandler.handle(Future.succeededFuture());
                    } else {
                        resultHandler.handle(Future.failedFuture(ar.cause()));
                    }
                });



    }

    public synchronized  void checkPlayerInput( String playerId,List<String> playerInput, long time) {

        boolean isCorrectInput = gsm.getCurrentSequenceAsString(gsm.getCurrentSequence()).equals(playerInput);

        gameModel.registerPlayerInput(playerId, isCorrectInput);

        PlayerInfo playerInfo= gsm.getPlayerInfos().get(playerId);
        if (isCorrectInput) {
            gameModel.getIsCorrect().put(playerId, true);
            playerInfo.updateRound(gameModel.currentRound);
            logger.info("Player's Time{}",playerInfo.totalMoveTime);
            updatePlayerData(playerInfo ,true, false);
            int actualScore = gsm.calculateScore(gameModel.currentRound, time);
            playerInfo.addScore(actualScore);
            gsm.insertPlayerScoreData(playerId, playerInfo ,  time);
            logger.info("Player's  data{}",playerInfo);
        }else{
            gsm.removeToReadyPlayer(playerId);
            gsm.insertPlayerScoreData(playerId, playerInfo ,  time);
            updatePlayerData(playerInfo,false,  true);


        }
        if(gameModel.allPlayersSubmitted()){
            if(gameModel.getQualifiedPlayers().isEmpty()){
                gameModel.endGame();
                logger.info("Game has ended.");
                endGameindb(gameModel.gameCode, dbRes -> {
                    if (dbRes.succeeded()) {
                        logger.info("Game ended and lock released. . Ich komme hier ");
                        insertAllScores(gsm.getPlayerScoreData());
                        gsm.getPlayerScoreData().clear();
                        logger.info(" playerScoreData  {}",gsm.getPlayerScoreData());
                        gsm.getEventBus().publish("group-24.simon.game.endGame" , new JsonObject().put("message","EndGame"));
                    } else {
                        releaseGameLock();
                        logger.error("Failed to mark game as ended in DB: " + dbRes.cause().getMessage());
                    }
                });
            } else {
                nextRound();
            }
        }

    }


    public  void  insertAllScores(Map<String ,PlayerInfo>playerScores){

        for (Map.Entry<String ,PlayerInfo> entry : playerScores.entrySet()) {
            String playerId = entry.getKey();
            PlayerInfo playerInfo = entry.getValue();
            int score = playerInfo.score;
            long duration = playerInfo.totalMoveTime;

            insertHighScore(Integer.parseInt(playerId),score,playerInfo.formatMillis(duration));
            logger.info("insert score " + score + " to " + playerId);
            logger.info("insert duration " + duration + " to " + playerId);

            logger.info("insert time  {} to :{}" , playerInfo.formatMillis(duration), playerId);
            playerInfo.resetPlayerInfo();
        }


    }

    public void updatePlayerData(PlayerInfo playerInfo, boolean status, boolean isEliminiert) {
        playerInfo.setReady(status);

        JsonObject json = new JsonObject();
                        json
                        .put("controllerId", playerInfo.controllerId)
                        .put("round", playerInfo.gameRound).put("status",  status)
                        .put("totalMoveTime", playerInfo.formatMillis(playerInfo.totalMoveTime)).put("score", playerInfo.score);


                gsm.getEventBus().publish("group-24.simon.game.players.progress", json);

                if (isEliminiert) {
                    gsm.getEventBus().publish("group-24.simon.game.players.eliminated", json);
                }

    }

    public void result (PlayerInfo playerInfo, boolean  isCorrect) {
        JsonObject json = new JsonObject();
        json.put("round", playerInfo.gameRound);
        String controllerId = playerInfo.controllerId;
        json.put("controllerId", controllerId);
        json.put("result", isCorrect ? "correct" : "wrong");
        gsm.getEventBus().publish("group-24.simon.game.players.feedback", json);

    }


    private void nextRound() {
        logger.info("next round gestartet {}" , gameModel);
        gsm.setCurrentSequence(gameModel.nextColors());
        JsonObject nextRoundMessage = new JsonObject()
                .put("round", gameModel.currentRound)
                .put("sequence", new JsonArray(gsm.getCurrentSequenceAsString(gsm.getCurrentSequence())))
                .put("inputTimeLimit",gsm.calculateInputTimeLimit(gsm.getCurrentSequence().size()))
                .put("controllerIds", new JsonArray(gsm.getReadyControllerForPlayer(gameModel.getQualifiedPlayers())));
        gsm.getEventBus().publish("group-24.game.sequence", nextRoundMessage);
        gameModel.eliminateUnqualifiedPlayers();
        gameModel.resetForNextRound();
        logger.info("next round gestartet {}" , gsm.getReadyControllerForPlayer(gameModel.getQualifiedPlayers()));
        logger.info("next round gestartet  with {}" , gsm.getCurrentSequence());
        logger.info("next round gestartet  with {}", nextRoundMessage);
    }


   private void insertHighScore(int playerId, int score, String time) {
       logger.info("raw duration millis = {}", time);

       String query = """
        INSERT INTO highScore (gameId,  score, playerId, duration)
        VALUES (?, ?, ?, ?)
        """;

        jdbcPool.preparedQuery(query)
                .execute(Tuple.of(gameModel.gameCode, score, playerId, time), ar -> {
                    if (ar.succeeded()) {
                        logger.info("Highscore updated for player " + playerId);
                    } else {
                        logger.error("Failed to update Highscore", ar.cause());
                    }
                });
    }
}

