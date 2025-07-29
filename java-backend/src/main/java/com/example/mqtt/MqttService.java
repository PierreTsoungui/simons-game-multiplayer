package com.example.mqtt;

import com.example.game.GameStateManager;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.codec.mqtt.MqttQoS;
import io.vertx.core.json.JsonObject;
import io.vertx.mqtt.MqttClient;

import java.util.List;

public class MqttService {

    private final MqttClient mqttClient;

    private static final Logger logger = LoggerFactory.getLogger(MqttService.class);
    final String mqttMessagePrefix = System.getenv("MQTT_MESSAGE_PREFIX") != null ? System.getenv("MQTT_MESSAGE_PREFIX") : "test/";

    public MqttService(MqttClient mqttClient) {
        this.mqttClient = mqttClient;
    }

    public void publishGameStart() {
        JsonObject data = new JsonObject().put("action", "start");
        mqttClient.publish(mqttMessagePrefix + "simon/game/start", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published game start: {}", data);
    }

    public void publishGameStop() {
        JsonObject data = new JsonObject().put("action", "stop");
        mqttClient.publish(mqttMessagePrefix + "simon/game/stop", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published game stop: {}", data);
    }

    public void publishObjectCreated(String name) {
        JsonObject data = new JsonObject().put("name", name);
        mqttClient.publish(mqttMessagePrefix + "simon/game/object/created", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);

        logger.info("📡 MQTT published object created: {}", data);
    }

    public void publishDemoMessage(String message) {
        JsonObject data = new JsonObject().put("message", message);
        mqttClient.publish(mqttMessagePrefix + "demo/message", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published demo message: {}", data);
    }
    public void publishRegisterControllers(List<String>controllers) {
        JsonArray activeCtrls = new JsonArray(controllers);
        JsonObject data = new JsonObject().put("activeControllers", activeCtrls);
        mqttClient.publish(mqttMessagePrefix + "simon/game/publishRegCtrls", data.toBuffer(), MqttQoS.AT_MOST_ONCE, false, false);
        logger.info("📡 MQTT published controllers: {}", data);
    }


    /// MqttService.java
    public void publishColorSequence(JsonArray sequence, int round) {
        JsonObject data = new JsonObject().put("sequence", sequence).put("round", round);
        mqttClient.publish(mqttMessagePrefix +"simon/game/sequence", data.toBuffer(), MqttQoS.AT_LEAST_ONCE, false, false);
    }
}
