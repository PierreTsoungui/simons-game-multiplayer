package com.example.highscore;

import com.example.http.HttpController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.player.PlayerController;


import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

import java.time.LocalTime;

public class HighscoreController implements HttpController {
    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);
    private final HighscoreService highscoreService;
    private final EventBus eventBus;

    public HighscoreController(Vertx vertx) {
        this.eventBus = vertx.eventBus();
        this.highscoreService = new HighscoreService();
    }

    public void registerRoutes(Router router) {
        router.get("/api/highscores").handler(this::handleHighscores);
    }

    private void handleHighscores(RoutingContext ctx){
        highscoreService.fetchHighscoreData(res -> {
            if (res.succeeded()) {
                JsonArray highscoreData = new JsonArray();
                res.result().forEach(row -> {
                    LocalTime duration = row.getLocalTime("duration");
                    JsonObject player = new JsonObject()
                            .put("playerName", row.getString("playerName"))
                            .put("score", row.getInteger("score"))
                            .put("totalMoveTime", duration.toString());
                    highscoreData.add(player);
                });
                ctx.response().putHeader("Content-Type", "application/json")
                        .setStatusCode(200)
                        .end(new JsonObject().put("highscoreData", highscoreData).encode());
            } else {
                ctx.response().setStatusCode(500).end("Failed to fetch high scores");
                logger.error("Failed to fetch high scores: {}", res.cause().getMessage());
            }
        });
    }

}
