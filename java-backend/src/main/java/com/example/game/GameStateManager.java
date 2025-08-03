package com.example.game;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.example.player.PlayerInfo;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

public class GameStateManager {

    private static final Logger logger = LoggerFactory.getLogger(GameStateManager.class);

    private final List<String> activePlayers = Collections.synchronizedList(new ArrayList<>());
    private final List<String> activeControllers = Collections.synchronizedList(new ArrayList<>());
    private final List<Integer> currentSequence = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, String> playerToController = new ConcurrentHashMap<>();
    private final Map<String, PlayerInfo> playerInfos  = new ConcurrentHashMap<>();

    private static GameStateManager instance;
    private final EventBus eventBus;

    private GameStateManager(Vertx vertx) {
        this.eventBus = vertx.eventBus();
    }

    public static synchronized GameStateManager getInstance(Vertx vertx) {
        if (instance == null) {
            instance = new GameStateManager(vertx);
        }
        return instance;
    }
    public Map<String, PlayerInfo> getPlayerInfos(){
        return playerInfos;
    }

    public List<String> getActiveControllers(){
        return activeControllers;
    }
    void addPlayer(String name) {
        if (!activePlayers.contains(name)) {
            activePlayers.add(name);
        }
    }

    void setCurrentSequence(List<Integer> sequence) {
        currentSequence.clear();
        currentSequence.addAll(sequence);
    }

    void removePlayer(String name) {
        activePlayers.remove(name);
    }

    EventBus getEventBus() {
        return eventBus;
    }

    List<String> getActivePlayers() {
        return activePlayers;
    }

    /**
     * Controller-Management
     */
    public void registerController(String controllerId) {
        if (controllerId != null && !controllerId.isBlank() && !activeControllers.contains(controllerId)) {
            activeControllers.add(controllerId);
            logger.debug("Controller registered: {}", controllerId);
        }
    }

    public boolean removeActiveControllers(String controllerId) {
        if (activeControllers.contains(controllerId)) {
            activeControllers.remove(controllerId);
            logger.info("Controller unregistered: {}", controllerId);
            return true;
        }else{
            logger.warn("Controller unregistered: {}", controllerId);
            return false;
        }
    }

    /**
     * Spieler-Controller-Zuordnung
     */
    public void assignPlayerToController(String playerId, String controllerId) {
        if (activePlayers.contains(playerId) && activeControllers.contains(controllerId)) {
            playerToController.put(playerId, controllerId);
            logger.info("Player {} assigned to controller {}", playerId, controllerId);
            eventBus.publish("player.assignment",
                    new JsonObject()
                            .put("player", playerId)
                            .put("controller", controllerId)
            );
        }
    }

    String getControllerIdForPlayer(String playerId) {
        return playerToController.getOrDefault(playerId, null);
    }


    public void addActivePlayers(String playerId) {
        if (playerId != null && !playerId.isBlank() && !activePlayers.contains(playerId)) {
            activePlayers.add(playerId);
            logger.info("Controller registered: {}", playerId);
        }
    }

    public void addActiveControllers(String controllerId) {
        if (controllerId != null && !controllerId.isBlank() && !activePlayers.contains(controllerId)) {
            activeControllers.add(controllerId);
            logger.debug("Controller registered: {}", controllerId);
        }

    }

    public boolean addPlayerToController(String playerId, String controllerId, String playerName) {
                logger.info("List of Controllers: {}", activeControllers);
                if(activeControllers.contains(controllerId)) {
                    addActivePlayers(playerId);
                    playerToController.put(playerId, controllerId);
                    playerInfos.put(playerId, new PlayerInfo(controllerId, 0, false, 0, playerName));
                    removeActiveControllers(controllerId);
                    JsonArray jsonArray = new JsonArray(activeControllers);
                    //JsonObject data = new JsonObject().put("activeControllers", new JsonArray(activeControllers));
                    this.eventBus.publish("group-24.simon.game.publishRegCtrls", jsonArray);
                    logger.info(" Successful coupling between: {} {}!", playerId, controllerId);
                    return true;
                }else{
                    logger.warn("Coupling between  : {} {} was unsuccessful!", playerId, controllerId);
                    return false;
                }
    }


    public  String playerIdFromControllerId(String controllerId) {

        for(Map.Entry<String,PlayerInfo> entry: playerInfos.entrySet()){
            if(entry.getValue().controllerId.equals(controllerId)){
                return entry.getKey();
            }

        }
        return null;
    }

}





