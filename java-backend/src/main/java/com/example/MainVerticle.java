package com.example;

import java.util.List;

import com.example.player.PlayerController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.database.DatabaseClient;
import com.example.game.GameStateManager;
import com.example.http.HttpController;
import com.example.http.HttpServerVerticle;
import com.example.mqtt.MqttVerticle;
import com.example.object.ObjectController;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class MainVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    @Override
    public void start(Promise<Void> startPromise) {
        setupJDBCPool();

        GameStateManager gsm =  GameStateManager.getInstance(vertx);

        MqttVerticle mqtt = new MqttVerticle();
        HttpServerVerticle http = new HttpServerVerticle();

        vertx.deployVerticle(mqtt);
        vertx.deployVerticle(http);

        setupHttpVerticle(http);

        logger.info("🚀 Alle Verticles und GameStateManager gestartet.");

        startPromise.complete();

    }

    private void setupJDBCPool() {
        JsonObject config = new JsonObject()
                .put("DB_HOST", System.getenv("DB_HOST") != null ? System.getenv("DB_HOST") : "mariadb")
                .put("DB_PORT", System.getenv("DB_PORT") != null ? Integer.parseInt(System.getenv("DB_PORT")) : 3306)
                .put("DB_NAME", System.getenv("DB_NAME") != null ? System.getenv("DB_NAME") : "simon_game")
                .put("DB_USER", System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "user")
                .put("DB_PASSWORD", System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "userpassword");

        DatabaseClient.initialize(vertx, config);
    }

    private void setupHttpVerticle(HttpServerVerticle httpVerticle) {

        final List<HttpController> controllers = List.of(
                new ObjectController(vertx),new PlayerController(vertx)
        );

        controllers.forEach(it -> it.registerRoutes(httpVerticle.router));
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new MainVerticle(), res -> {
            if (res.succeeded()) {
                logger.info("Verticle deployment succeeded");
            } else {
                logger.error("Verticle deployment failed: {}", res.cause().getMessage());
            }
        });
    }
}
