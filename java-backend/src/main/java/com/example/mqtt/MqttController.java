package com.example.mqtt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.example.game.GameService;
import com.example.game.GameStateManager;
import com.example.player.PlayerInfo;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.mariadb.jdbc.util.ParameterList;
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
    GameService service;

    public MqttController(MqttClient mqttClient, Vertx vertx) {
        this.mqttClient = mqttClient;
        this.mqttService = new MqttService(mqttClient);
        this.eventBus = vertx.eventBus();
        this.gsm = GameStateManager.getInstance(vertx);
        this.service = GameService.getInstance(vertx);

    }

    /* EVENTS empfangen und verarbeiten */
    public void registerEventBusConsumers() {
        this.eventBus.consumer("group-24.simon.game.start", msg -> {
            JsonObject data = (JsonObject) msg.body();
            JsonArray controllersArray = data.getJsonArray("controllerIds");
            List<String> controllerIds= gsm.getControllerIdFromJsonBody(controllersArray);
            data.remove("controllerIds");
            logger.info("Message received via EventBus: 'group-24.simon.game.start'");
            mqttService.publishGameStart(data, controllerIds);
        });

        this.eventBus.consumer("group-24.simon.game.available.controller", msg -> {
            JsonObject data = (JsonObject) msg.body();
            JsonArray controllersArray = data.getJsonArray("controllerIds");
            List<String> controllerIds= gsm.getControllerIdFromJsonBody(controllersArray);
            mqttService.publishRegisterControllers(controllerIds);
        });
        this.eventBus.consumer("group-24.simon.game.info", msg -> {
            JsonObject data = (JsonObject) msg.body();
            JsonArray controllersArray = data.getJsonArray("controllerIds");

            List<String> controllerIds= gsm.getControllerIdFromJsonBody(controllersArray);

            logger.info("controller in startMessage: {}", controllerIds);
            data.remove("controllerIds");
            mqttService.publishGameInfo(data, controllerIds);
        });
        this.eventBus.consumer("game.stop", msg -> {
            logger.info("Message received via EventBus: 'game.stop'");
            mqttService.publishGameStop();
        });
        this.eventBus.consumer("group-24.game.sequence", msg -> {
            JsonObject data = (JsonObject) msg.body();

            JsonArray controllersArray = data.getJsonArray("controllerIds");
            List<String> controllerIds= gsm.getControllerIdFromJsonBody(controllersArray);
            data.remove("controllerIds");

            mqttService.publishColorSequence(data,controllerIds); // → MQTT

            logger.info("Message received via EventBus: 'group-24.simon.game.sequence'");
        });

        this.eventBus.consumer("object.created", msg -> {
            logger.info("Message received via EventBus: 'object.created'");
            mqttService.publishObjectCreated(msg.body().toString());
        });
        this.eventBus.consumer("mqtt.demo.message", msg -> {
            logger.info("Message received via EventBus: 'mqtt.message'");
            mqttService.publishDemoMessage(msg.body().toString());
        });
        this.eventBus.consumer("group-24.simon.game.publishRegCtrls", msg -> {
            logger.info("Message received via EventBus: 'game.publishRegCtrls'");
            JsonArray controllers = (JsonArray) msg.body();
            @SuppressWarnings("unchecked")
            List<String> controllerList = controllers.getList();
            mqttService.publishRegisterControllers(controllerList);

        });

        this.eventBus.consumer("group-24.simon.game.events.playerJoined", msg -> {
            logger.info("Message received via EventBus: 'game.publishPlayerJoined'");
            JsonObject playerData = (JsonObject) msg.body();
            mqttService.publishJoinedPlayerInfo(playerData);
        });
        this.eventBus.consumer("group-24.simon.game.players.progress", msg -> {
            logger.info("Message received via EventBus: 'group-24.simon.game.players.progress'");
            JsonObject playerData = (JsonObject) msg.body();
            mqttService.publishPlayerProgress(playerData);
        });
        this.eventBus.consumer("group-24.simon.game.players.loggedIn", msg -> {
            logger.info("Message received via EventBus: 'group-24.simon.game.players.loggedIn'");
            JsonObject playerData = (JsonObject) msg.body();
            mqttService.publishPlayerData( playerData);
        });

        this.eventBus.consumer("group-24.simon.game.players.eliminated", msg -> {
            logger.info("Message received via EventBus: 'group-24.simon.game.players.eliminated'");
            JsonObject playerData = (JsonObject) msg.body();
            mqttService.publishPlayerElimination( playerData);
        });

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
                    }else if(topic.startsWith(mqttMessagePrefix + "simon/game/player/status/")) {
                        logger.info("Simon  controllerStatus message received: {}", payload.toString());
                        String prefix=mqttMessagePrefix + "simon/game/player/status/";
                        String controllerId= topic.substring(prefix.length());

                        String playerId= gsm.playerIdFromControllerId(controllerId);
                        logger.info("PlayerId: {} on {}", playerId,controllerId);
                        if(playerId != null){
                            PlayerInfo infos= gsm.getPlayerInfos().get(playerId);
                           boolean status=  Boolean.parseBoolean(payload.toString());
                           infos.setReady(status);
                           if(infos.isReady()){

                               gsm.addToReadyPlayer(playerId);
                           }else{
                               gsm.removeToReadyPlayer(playerId);
                           }
                            mqttService.publishStatusUpdate(controllerId, status);
                        }

                    }else if(topic.startsWith(mqttMessagePrefix + "simon/game/input/")) {
                        logger.info("User's input successfully received: {}!", payload.toString());

                        // Schritt 2: JSON parsen
                        JsonObject obj = new JsonObject( payload.toString());

                        // Schritt 3: Daten extrahieren
                        JsonArray input = obj.getJsonArray("input");
                        long duration = obj.getLong("timeInMillis");
                        boolean completed = obj.getBoolean("complete");

                        // Wandlung von JsonArray zu List<String>
                        List<String> inputList = input.stream()
                                .map(Object::toString)
                                .collect(Collectors.toList());

                        // Schritt 4: Controller-ID aus Topic
                        String[] parts = topic.split("/");
                        String controllerId = parts[4];

                        service.checkPlayerInput(gsm.playerIdFromControllerId(controllerId), inputList, duration);
                        logger.info("time {}", duration);
                        logger.info("completed {}", completed);
                        logger.info("controllerId {}", controllerId);

                    }else if(topic.equals(mqttMessagePrefix + "simon/game/disconnect")) {
                        logger.info("Simon  game  disconnect info received: {}", payload.toString());
                        JsonObject obj = new JsonObject(payload.toString());
                        String controllerId = obj.getString("controllerId");
                        String playerId = gsm.playerIdFromControllerId(controllerId);
                        if(playerId != null){
                            gsm.generalPlayerRemoveMethod(playerId);
                            mqttService.publishUpdateWaitingArea(PlayerInfo.dataWaitingArea(gsm.getPlayerInfos()));
                        }
                        gsm.removeActiveControllers(controllerId);

                        mqttService.publishRegisterControllers(gsm.getActiveControllers());

                    }
                    else {
                        logger.info("Handler for topic: {} not implemented", topic);
                    }
        });

        mqttClient.subscribe(Map.of(
                mqttMessagePrefix + "demo/hello_world", 0,
                mqttMessagePrefix + "output", 0,
                mqttMessagePrefix + "simon/game/registerController", 0,
                mqttMessagePrefix + "simon/game/player/status/+",0,
                mqttMessagePrefix + "simon/game/input/+", 0,
                mqttMessagePrefix +"simon/game/disconnect",0
        ));

    }
}
