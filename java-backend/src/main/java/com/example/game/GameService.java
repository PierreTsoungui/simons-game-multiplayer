package com.example.game;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GameService {

GameModel gameModel;
GameStateManager gameStateManager;
Map<String,String> readyPlayers;

GameService() {
    gameStateManager =GameStateManager.getInstance(Vertx.vertx());
}


    void startGame() {
        gameModel = new GameModel(new ArrayList<>(readyPlayers.keySet()));
        gameModel.addColorToSequence();
        gameStateManager.setCurrentSequence(gameModel.colorSequence);

        JsonObject eventData = new JsonObject()
                .put("sequence", new JsonArray(gameModel.colorSequence))
                .put("round", gameModel.currentRound);

        gameStateManager.getEventBus().publish("game.sequence", eventData);
    }

    void nextRound() {
        if (gameModel.isGameActive) {
            gameModel.addColorToSequence();
            gameStateManager.setCurrentSequence(gameModel.colorSequence);

            JsonObject eventData = new JsonObject()
                    .put("sequence", new JsonArray(gameModel.getColorSequenceAsNames()))
                    .put("round", gameModel.currentRound);

            gameStateManager.getEventBus().publish("game.sequence", eventData);
        }
    }


void processPlayerInput(String playerId, List<Integer> playerInput) {
    if(gameModel.checkPlayerInput(playerId,playerInput)) {
        gameStateManager.getEventBus().publish("game.input.correct",playerId);

    }else{
        gameStateManager.getEventBus().publish("game.input.wrong",playerId);
        gameStateManager.removePlayer(playerId);

        if(gameStateManager.getActivePlayers().isEmpty()){
            gameModel.isGameActive=false;
        }
    }

}



}
