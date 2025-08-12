package com.example.player;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.stream.Collector;

public class PlayerInfo {
    public String playerName;
    public String controllerId;
    public int gameRound;
    public  boolean ready;
    public  long totalMoveTime; // in Sekunden oder Millisekunden
    public  int score;

    public PlayerInfo(String controllerId, int gameRound, boolean ready, long totalMoveTime, String playerName, int score) {
        this.controllerId = controllerId;
        this.gameRound = gameRound;
        this.ready = ready;
        this.totalMoveTime = totalMoveTime;
        this.playerName = playerName;
        this.score = score;
    }


    // --- Getter und Setter ---

    public String getControllerId() {
        return controllerId;
    }

    public void setControllerId(String controllerId) {
        this.controllerId = controllerId;
    }

    public void setGameRound(int gameRound) {
        this.gameRound = gameRound;
    }

    public boolean isReady() {
        return ready;
    }


    public void setReady(boolean status) {
        this.ready = status;
    }

    public long getTotalMoveTime() {
        return totalMoveTime;
    }

    public void setTotalMoveTime(long totalMoveTime) {
        this.totalMoveTime = totalMoveTime;
    }

    // Time incrementation
    public void addMoveTime(long moveTime) {
        this.totalMoveTime += moveTime;
    }
    public  void addScore(int score) {
        this.score += score;
    }

    public void updateRound( int round) {
        this.gameRound= round;
    }
    public String formatMillis(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }
    public static JsonArray dataWaitingArea(Map<String, PlayerInfo> playerInfos) {
         return playerInfos.entrySet().stream()
                .map(entry -> {
                    PlayerInfo infos = entry.getValue();

                    JsonObject json = new JsonObject()
                            .put("playerName", infos.playerName)
                            .put("controllerId", infos.controllerId)
                            .put("status", infos.ready)
                            .put("round", infos.gameRound)
                            .put("totalMoveTime", infos.formatMillis(infos.totalMoveTime));

                    return json;
                })
                .collect(Collector.of(
                        JsonArray::new,
                        JsonArray::add,
                        JsonArray::addAll
                ));

    }


   public  static JsonObject jsonFromPlayer(PlayerInfo playerInfo) {
        JsonObject json = new JsonObject();
        json.put("playerName", playerInfo.playerName);
        json.put("controllerId", playerInfo.controllerId);
        json.put("gameRound", playerInfo.gameRound);
        json.put("ready", playerInfo.ready);
        json.put("totalMoveTime", playerInfo.totalMoveTime);

        return json;


    }

    public  void resetPlayerInfo() {
        this.gameRound =0;
        this.ready = false;
        this.totalMoveTime = 0;
        this.score = 0;
    }

    @Override
    public String toString() {
       return   "PlayerTime:["+ this.totalMoveTime +"] , PlayerScore :[" + this.score + "]";

    }

}