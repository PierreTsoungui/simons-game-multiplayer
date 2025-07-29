package com.example.mqtt;

import java.util.Map;

import com.example.game.GameStateManager;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.eventbus.EventBus;
import io.vertx.mqtt.MqttClient;

public class MqttController {

    private static final Logger logger = LoggerFactory.getLogger(MqttController.class);
    private final MqttService mqttService;
    private final MqttClient mqttClient;
    private final GameStateManager gsm;
    private final EventBus eventBus;
    final String mqttMessagePrefix = System.getenv("MQTT_MESSAGE_PREFIX") != null ? System.getenv("MQTT_MESSAGE_PREFIX") : "group-24/";

    public MqttController(MqttClient mqttClient, Vertx vertx) {
        this.mqttClient = mqttClient;
        this.mqttService = new MqttService(mqttClient);
        this.eventBus = vertx.eventBus();
        this.gsm = GameStateManager.getInstance(vertx);

    }

    /* EVENTS empfangen und verarbeiten */
    public void registerEventBusConsumers() {
        this.eventBus.consumer("game.start", msg -> {
            logger.info("Message received via EventBus: 'game.start'");
            mqttService.publishGameStart();
        });

        this.eventBus.consumer("game.stop", msg -> {
            logger.info("Message received via EventBus: 'game.stop'");
            mqttService.publishGameStop();
        });
        this.eventBus.consumer("game.sequence", msg -> {
            JsonObject data = (JsonObject) msg.body();
            mqttService.publishColorSequence(data.getJsonArray("sequence"), data.getInteger("round")); // → MQTT
        });
        this.eventBus.consumer("object.created", msg -> {
            logger.info("Message received via EventBus: 'object.created'");
            mqttService.publishObjectCreated(msg.body().toString());
        });
        this.eventBus.consumer("mqtt.demo.message", msg -> {
            logger.info("Message received via EventBus: 'mqtt.message'");
            mqttService.publishDemoMessage(msg.body().toString());
        });
        /*this.eventBus.consumer("simon.game.publishRegCtrls", msg -> {
            logger.info("Message received via EventBus: 'game.publishRegCtrls'");
            mqttService.publishRegisterControllers(msg.body().toString());
        });*/
    }

    public void registerMqttConsumers() {

        mqttClient.publishHandler(message -> {

                    Buffer payload = message.payload();
                    String topic = message.topicName();

                    logger.info("Message received via Mqtt. Topic: {}, Payload: {}", topic, payload);

                    if (topic.equals(mqttMessagePrefix + "demo/hello_world")) {
                        this.eventBus.publish("mqtt.demo.message", payload);
                    } else if (topic.equals(mqttMessagePrefix + "output")) {
                        logger.info("Output message received: {}", payload.toString());
                    } else if(topic.equals(mqttMessagePrefix + "simon/game/registerController")) {
                        logger.info("Simon controller message received: {}", payload.toString());
                        gsm.addActiveControllers(payload.toString());
                        mqttService.publishRegisterControllers(gsm.getActiveControllers());
                    }else {
                        logger.info("Handler for topic: {} not implemented", topic);
                    }

        });

        mqttClient.subscribe(Map.of(
                mqttMessagePrefix + "demo/hello_world", 0,
                mqttMessagePrefix + "output", 0,
                mqttMessagePrefix + "simon/game/registerController", 0

        ));

    }
}
