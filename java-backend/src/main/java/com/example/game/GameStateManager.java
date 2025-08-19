package com.example.game;

import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.example.player.PlayerInfo;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;

public class GameStateManager {
    public static final Map<Integer, String> COLOR_MAP = Map.of(
            0, "RED",
            1, "GREEN",
            2, "BLUE",
            3, "YELLOW"
    );
    private static final Logger logger = LoggerFactory.getLogger(GameStateManager.class);

    private final List<String> activePlayers = Collections.synchronizedList(new ArrayList<>());
    private final List<String> activeControllers = Collections.synchronizedList(new ArrayList<>());
    private final List<Integer> currentSequence = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, String> playerToController = new ConcurrentHashMap<>();
    private final Map<String, PlayerInfo> playerInfos = new ConcurrentHashMap<>();
    private final List<String> readyPlayers = Collections.synchronizedList(new ArrayList<>());
    private final  Map<String, PlayerInfo> playerScoreData = new ConcurrentHashMap<>();

    private static GameStateManager instance;
    private final EventBus eventBus;
    private final Vertx vertx;

    private GameStateManager(Vertx vertx) {
        this.eventBus = vertx.eventBus();
        this.vertx = vertx;
    }




    public static synchronized GameStateManager getInstance(Vertx vertx) {
        if (instance == null) {
            instance = new GameStateManager(vertx);
        }
        return instance;
    }
    public  Vertx getVertx() {

        return vertx;
    }
    public Map<String, PlayerInfo> getPlayerInfos() {
        return playerInfos;
    }

    public List<String> getActiveControllers() {
        return activeControllers;
    }

public  void generalPlayerRemoveMethod(String playerId) {
    removePlayerToController(playerId);
    removeActivePlayer(playerId);
    playerInfos.remove(playerId);
}

public Map<String, PlayerInfo> getPlayerScoreData() {
     return  playerScoreData ;
}

    public void insertPlayerScoreData(String playerId, PlayerInfo playerInfo, long time ) {
        playerInfo.addMoveTime(time);
        playerScoreData.put(playerId, playerInfo);
    }

   public void setCurrentSequence(int color) {

        currentSequence.add(color);
    }
   public List<Integer>getCurrentSequence() {

        return this.currentSequence;
    }

     public List<String> getCurrentSequenceAsString(List<Integer> colorSequence) {
        return colorSequence.stream()
                .map(COLOR_MAP::get)
                .toList();
    }

    void resetCurrentSequence() {

        this.currentSequence.clear();
    }
   public EventBus getEventBus() {

        return eventBus;
    }


    /**
     * Controller-Management
     */


    public  void removeActiveControllers(String controllerId) {
        if (activeControllers.contains(controllerId)) {
            activeControllers.remove(controllerId);
            logger.info("Controller unregistered: {}", controllerId);

        } else {
            logger.warn("Controller unregistered: {}", controllerId);

        }
    }

    public boolean removeActivePlayer(String playerId) {
        if (activePlayers.contains(playerId)) {
            activePlayers.remove(playerId);
            return true;
        } else {
            return false;
        }
    }

    public void removePlayerToController(String playerId) {

        playerToController.remove(playerId);
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

    public String getControllerIdForPlayer(String playerId) {

        return playerToController.getOrDefault(playerId, null);
    }


    public void addActivePlayers(String playerId) {
        if (playerId != null && !playerId.isBlank() && !activePlayers.contains(playerId)) {
            activePlayers.add(playerId);
            logger.info("Controller registered: {}", playerId);
        }
    }

    public void addActiveControllers(String controllerId) {
        if (controllerId != null && !controllerId.isBlank() && !activeControllers.contains(controllerId)) {
            activeControllers.add(controllerId);
            logger.debug("Controller registered successfully: {}", controllerId);
        }

    }

    public boolean addPlayerToController(String playerId, String controllerId, String playerName) {
        logger.info("List of Controllers: {}", activeControllers);
        if (activeControllers.contains(controllerId) && !activePlayers.contains(playerId)) {
            addActivePlayers(playerId);
            playerToController.put(playerId, controllerId);
            playerInfos.put(playerId, new PlayerInfo(controllerId, 0, false, 0, playerName,0));
            removeActiveControllers(controllerId);
            JsonArray jsonArray = new JsonArray(activeControllers);
            this.eventBus.publish("group-24.simon.game.publishRegCtrls", jsonArray);
            logger.info(" Successful coupling between: {} {}!", playerId, controllerId);
            return true;
        } else {
            logger.warn("Coupling between  : {} {} was unsuccessful!", playerId, controllerId);
            return false;
        }
    }


    public synchronized  String playerIdFromControllerId(String controllerId) {
        for (Map.Entry<String, String> entry : playerToController .entrySet()) {
            if (entry.getValue().equals(controllerId)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public List<String> getReadyPlayers() {
        return readyPlayers;
    }

    public void addToReadyPlayer(String playerId) {
        if (playerId == null || playerId.isBlank() ) {
            logger.error("Invalid playerId");
            return;
        }

        PlayerInfo playerInfo = playerInfos.get(playerId);
        if (playerInfo == null || !playerInfo.isReady()) {
            logger.error("PlayerInfo is null or player not ready for playerId {}", playerId);
            return;
        }
        synchronized (readyPlayers) {
            if (!readyPlayers.contains(playerId)) {
                readyPlayers.add(playerId);
                logger.info("Player {} is ready and added to the ready player's list", playerId);
            } else {
                logger.warn("Player {} already in the ready list", playerId);
            }
        }
    }

     public  List<String> getReadyControllerForPlayer(List<String> readyPlayers) {
        List<String> readyControllers = new ArrayList<>();
        for (String playerId : readyPlayers) {
            readyControllers.add(getControllerIdForPlayer(playerId));
        }
        return readyControllers;
     }


    public List<String>getControllerIdFromJsonBody(JsonArray jsonArray) {
        return jsonArray.stream().map(Object::toString).collect(Collectors.toList());
    }


    /**
     *   entfernt eines Players aus der List von readyPlayer
     */
    public void removeToReadyPlayer(String playerId) {
        synchronized (readyPlayers) {
            readyPlayers.remove(playerId);

            logger.info("Player {}  remove To ReadyList Successfully", playerId);
        }
    }


    /**
     *   verteilt die TimeInput je nach  SequenceLength
     */
    public int calculateInputTimeLimit(int sequenceLength) {
        double timePerColor;
        if (sequenceLength <= 1) {
            timePerColor = 12.0;
        } else if (sequenceLength <= 2) {
            timePerColor = 1.5;
        } else if (sequenceLength <= 4) {
            timePerColor = 1.2;
        } else {
            timePerColor = 1.0;
        }

        return (int) (sequenceLength * timePerColor * 1000);
    }

    /**
     *  Zur Berechnung der Punkte  für Ein Spieler
     */
    public int calculateScore(int round, long reactionTimeMillis) {
        final int maxReactionTime = 3000;

        if (reactionTimeMillis > maxReactionTime) return 0;

        // Punkte = Rundenfaktor * Zeitbonus
        int baseScore = round * 10;

        // Zeitbonus (je schneller, desto besser)
        double timeFactor = (maxReactionTime - reactionTimeMillis) / (double) maxReactionTime;

        // Gesamtpunktzahl
        return (int)(baseScore * timeFactor);
    }

}




