package com.example.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameModel {

    private final Map<String, Boolean> playerInputs = new ConcurrentHashMap<>();
    private final Map<String, Boolean> isCorrect = new ConcurrentHashMap<>();
    private final Map<String, Boolean> hasSubmitted = new ConcurrentHashMap<>();
    String gameCode;
    int currentRound;
    boolean isGameActive;
    List<String> players;
    Map<String, Integer> playerScores= new ConcurrentHashMap<>();

    public GameModel(List<String>players, String gameCode) {
        this.gameCode = gameCode;
        currentRound=0;
        isGameActive= true;
        this.players =players;
    }

int nextColors() {
    int color = (int) (Math.random() * 4);
    currentRound++;
    return color;
}

public Map<String, Boolean> getIsCorrect() {
        return isCorrect;
}

public void endGame() {
        this.isGameActive = false;
    }

    public void registerPlayerInput(String playerId, boolean correct) {
        hasSubmitted.put(playerId, true);
        isCorrect.put(playerId, correct);
    }

    public boolean allPlayersSubmitted() {
        for (String player : players) {
            if (!hasSubmitted.getOrDefault(player, false)) {
                return false;
            }
        }
        return true;
    }

    public List<String> getQualifiedPlayers() {
        List<String> qualified = new ArrayList<>();
        for (String player : players) {
            if (isCorrect.getOrDefault(player, false)) {
                qualified.add(player);
            }
        }
        return qualified;
    }
    public void eliminateUnqualifiedPlayers() {
        List<String> qualified = getQualifiedPlayers();
        players.clear();
        players.addAll(qualified);
    }

    public void resetForNextRound() {
        hasSubmitted.clear();
        isCorrect.clear();
    }

}
