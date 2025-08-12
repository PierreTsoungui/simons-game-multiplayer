package com.example.highscore;

import com.example.player.PlayerRepository;

import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;

public class HighscoreService {

    private final HighscoreRepository highscoreRepository;

    HighscoreService() {
        this.highscoreRepository = new HighscoreRepository();
    }


    void fetchHighscoreData(Handler<AsyncResult<RowSet<Row>>> resultHandler) {
        highscoreRepository.fetchHighscoreData(resultHandler);
    }
}