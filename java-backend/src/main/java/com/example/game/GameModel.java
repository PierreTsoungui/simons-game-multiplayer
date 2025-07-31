package com.example.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameModel {
    // Farbkonstanten hinzufügen
    public static final Map<Integer, String> COLOR_MAP = Map.of(
            0, "RED",
            1, "GREEN",
            2, "BLUE",
            3, "YELLOW"
    );
        int currentRound;
        boolean isGameActive;
        List<Integer> colorSequence;
        List<String> players;
        Map<String, String> playerTocontroller;
        Map<String, Integer> playerScores;
        Map<String, Double> playerPerformance;
        long startTime;
    public GameModel(List<String>players) {
        currentRound=0;
        isGameActive= true;
        colorSequence = new ArrayList<>();
        playerTocontroller= new HashMap<>();
        this.players =players;
        startTime = System.currentTimeMillis();

    }


void  addColorToSequence() {
    int color = (int) (Math.random() * 4);
    colorSequence.add(color);
    currentRound++;
}

boolean checkPlayerInput( String playerId,List<Integer> playerInput) {

        boolean isCorrectInput = colorSequence.equals(playerInput);

        if (isCorrectInput) {
             Double inputTime= (System.currentTimeMillis() - startTime) / 1000.0;
             playerPerformance.put(playerId,playerPerformance.getOrDefault(playerId,0.0)+inputTime);

        }
         return isCorrectInput;

    }

    public void endGame() {
        this.isGameActive = false;
    }

    public List<String> getColorSequenceAsNames() {
        return colorSequence.stream()
                .map(COLOR_MAP::get)
                .toList();
    }
}
