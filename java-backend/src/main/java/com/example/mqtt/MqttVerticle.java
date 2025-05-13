package com.example.mqtt;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.AbstractVerticle;
import io.vertx.mqtt.MqttClient;
import io.vertx.mqtt.MqttClientOptions;

public class MqttVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(MqttVerticle.class);

    @Override
    public void start() {

        MqttClientOptions options = new MqttClientOptions()
                .setAutoKeepAlive(true)
                .setUsername(System.getenv("MQTT_USERNAME") != null ? System.getenv("MQTT_USERNAME") : "your_mqtt_username")
                .setPassword(System.getenv("MQTT_PASSWORD") != null ? System.getenv("MQTT_PASSWORD") : "your_mqtt_password");

        MqttClient mqttClient = MqttClient.create(vertx, options);

        mqttClient.connect(1883, "mosquitto", ar -> {
            if (ar.succeeded()) {
                logger.info("Connected to MQTT broker successfully!");

                MqttController mqttController = new MqttController(mqttClient, vertx);
                mqttController.registerEventBusConsumers();
                mqttController.registerMqttConsumers();

            } else {
                logger.error("Failed to connect to MQTT: {}", ar.cause().getMessage());
            }
        });

    }
}
