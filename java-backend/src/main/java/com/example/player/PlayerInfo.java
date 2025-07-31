package com.example.player;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Map;
import java.util.stream.Collector;

public class PlayerInfo {

    public String controllerId;
    public int gameRound;
    public  boolean ready;
    public  long totalMoveTime; // in Sekunden oder Millisekunden

    public PlayerInfo(String controllerId, int gameRound, boolean ready, long totalMoveTime) {
        this.controllerId = controllerId;
        this.gameRound = gameRound;
        this.ready = ready;
        this.totalMoveTime = totalMoveTime;
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


    public void setReady(boolean ready) {
        this.ready = ready;
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

    // Round incrementation
    public void nextRound() {
        this.gameRound++;
    }
    public String formatMillis(long millis) {
        long seconds = millis / 1000;
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long remainingSeconds = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, remainingSeconds);
    }
    public JsonArray dataWaitingArea(Map<String, PlayerInfo> playerInfos) {
        JsonArray jsonArray = playerInfos.entrySet().stream()
                .map(entry -> {
                    String playerName = entry.getKey();
                    PlayerInfo infos = entry.getValue();

                    JsonObject json = new JsonObject()
                            .put("playerName", playerName)
                            .put("controllerId", infos.controllerId)
                            .put("status", infos.ready)
                            .put("round", infos.gameRound)
                            .put("totalMoveTime", infos.totalMoveTime);

                    return json;
                })
                .collect(Collector.of(
                        JsonArray::new,
                        JsonArray::add,
                        JsonArray::addAll
                ));
        return jsonArray;
    }
/*
    @Override
    public String toString() {
        return "PlayerInfo{" +
                "controllerId='" + controllerId + '\'' +
                ", gameRound=" + gameRound +
                ", ready=" + ready +
                ", totalMoveTime=" + totalMoveTime +
                '}';
    }*/
}