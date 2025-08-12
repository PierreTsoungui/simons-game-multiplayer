package com.example.mqtt;

import com.example.database.DatabaseClient;
import com.example.game.GameService;
import com.example.game.GameStateManager;
import com.sun.tools.jconsole.JConsoleContext;
import io.vertx.core.Vertx;
import io.vertx.jdbcclient.JDBCPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class MqttVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MqttVerticle.class);
GameService gameService= GameService.getInstance(vertx);

    @Override
    public void start() {


        String mqttBrokerUrl = System.getenv("MQTT_BROKER_URL") != null ? System.getenv("MQTT_BROKER_URL") : "ws://iti-mqtt.mni.thm.de:9001";
        int mqttBrokerPort = System.getenv("MQTT_BROKER_PORT") != null ? Integer.parseInt(System.getenv("MQTT_BROKER_PORT")) : 1883;
        String mqttUsername = System.getenv("MQTT_USERNAME") != null ? System.getenv("MQTT_USERNAME") : "group-24/";
        String mqttPassword = System.getenv("MQTT_PASSWORD") != null ? System.getenv("MQTT_PASSWORD") : "yX4k#sL8yNp1D";
        MqttClientOptions options = new MqttClientOptions()
                .setAutoKeepAlive(true)
                .setUsername(mqttUsername)
                .setPassword(mqttPassword);

        MqttClient mqttClient = MqttClient.create(vertx, options);

        JDBCPool jdbcPool = DatabaseClient.getInstance();

        String cleanupQuery = """
        UPDATE game
        SET isGameActive = FALSE,
            endTime = CURRENT_TIME()
        WHERE isGameActive = TRUE
          AND endTime IS NULL
          OR TIMESTAMPDIFF(MINUTE, startTime, CURRENT_TIME()) > 30
    """;
        jdbcPool
                .preparedQuery(cleanupQuery)
                .execute()
                .onSuccess(rows -> {
                    logger.info("Hängende Spiele wurden beendet: {} Spiele geändert", rows.rowCount());

                   // gameService.checkAndHandleActiveGames(ag -> {
                       // if (ag.succeeded()) {
                                gameService.releaseGameLock();
                            mqttClient.connect(mqttBrokerPort, mqttBrokerUrl, ar -> {
                                if (ar.succeeded()) {
                                    logger.info("Hängende Spiele wurden beendet: {} Spiele geändert", rows.rowCount());
                                    logger.info("Connected to MQTT broker successfully!{},{}",mqttBrokerUrl, mqttBrokerPort);

                                    MqttController mqttController = new MqttController(mqttClient, vertx);
                                    mqttController.registerEventBusConsumers();
                                    mqttController.registerMqttConsumers();

                                } else {

                                    logger.error("Failed to connect to MQTT + port {}: {}", mqttBrokerPort, ar.cause().getMessage());
                                }
                            });

                   //    } else {
                           // logger.error("Failed to handle Active games: {}", ag.cause().getMessage());
                       // }
                 //   });
                });
    }
}
