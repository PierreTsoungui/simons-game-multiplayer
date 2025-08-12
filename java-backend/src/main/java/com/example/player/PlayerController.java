package com.example.player;

import com.example.game.GameService;
import com.example.game.GameStateManager;
import com.example.http.HttpController;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.mqtt.MqttClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PlayerController implements HttpController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);
    private final PlayerService playerService;
    private final EventBus eventBus;
    private final GameStateManager gsm;
    private final GameService gs;

    public PlayerController(Vertx vertx) {
        this.eventBus = vertx.eventBus();
        this.playerService = new PlayerService();
        this.gsm = GameStateManager.getInstance(vertx);
        this.gs = GameService.getInstance(vertx);


    }

    public void registerRoutes(Router router) {
        router.post("/api/players/register").handler(this::handleCreate);
        router.post("/api/players/login").handler(this::handleLogin);
        router.get("/api/waitingArea").handler(this::handleWaitingArea);
        router.get("/api/controller/active").handler(this::getActiveController);
        router.get("/api/players/logout").handler(this::handleLogout);
        router.post("/api/game/start").handler(this::handleStartGame);
        router.patch("/api/players/updateProfile").handler(this::handleProfileUpdate);
        router.post("/api/controller/disconnect").handler(this::handleControllerDisconnect);


    }

    private void handleCreate(RoutingContext ctx) {
        JsonObject jsonBody = ctx.body().asJsonObject();

        logger.debug("Ich habe die Nachricht empfängt;");
        try {
            if (jsonBody == null) {
                ctx.response().setStatusCode(400).end("Request body must be JSON");
                return;
            }

            String userName = jsonBody.getString("userName");
            String password = jsonBody.getString("password");
            if (userName == null || userName.isBlank() || password == null || password.isBlank()) {
                ctx.response().setStatusCode(400).end("Username or password is missing");
                return;
            }

            playerService.createPlayer(userName, password, res -> {
                if (res.succeeded()) {
                    ctx.response().setStatusCode(201).putHeader("content-type", "application/json")
                            .end(new JsonObject().put("message", "Player created successfully").encode());
                    logger.debug("Created player in database");
                } else {
                    ctx.response()
                            .setStatusCode(500)
                            .end("Failed to create player in database");
                    logger.error("Failed to create player in database");
                }
            });


        } catch (Exception e) {
            logger.error("Error while parsing JSON body: {}", e.getMessage());
            ctx.response().setStatusCode(400).end("Error while parsing JSON body");
        }
    }

    private void handleLogin(RoutingContext ctx) {

        JsonObject json = ctx.body().asJsonObject();
        logger.info("Request body: {}", ctx.body().asString());


        try {
            handleVerifications(json, ctx);

            String playerName = json.getString("playerName");
            String password = json.getString("password");

            String controllerId = json.getString("controllerId");// comes here as web controller's id from frontend

            playerService.playerLogin(playerName, password, controllerId, gsm, res -> {
                if (res.succeeded()) {

                    PlayerInfo p = new PlayerInfo(controllerId, 0, false, 0, playerName,0);
                    JsonObject js = PlayerInfo.jsonFromPlayer(p);
                    String playerId = gsm.playerIdFromControllerId(controllerId);
                    ctx.response().setStatusCode(201).putHeader("content-type", "application/json")
                            .end(new JsonObject()
                                    .put("message", "Login successful").put("playerId", playerId).put("playerName", playerName).encode());
                    gsm.getEventBus().publish("group-24.simon.game.players.loggedIn", new JsonObject().put("playerName", playerName).put("controllerId", controllerId));
                    logger.debug("Login successful");
                    logger.info("Event published: player.logged in with name: {}", playerName);

                    this.eventBus.publish("group-24.simon.game.events.playerJoined", new JsonObject().put(" waitingAreaData", js));
                } else {
                    String errorMsg = res.cause() != null ? res.cause().getMessage() : "Unknown error";
                    ctx.response()
                            .putHeader("Content-Type", "application/json")
                            .setStatusCode(400)
                            .end(new JsonObject().put("error", errorMsg).encode());
                    logger.error("Failed to login player : {}", errorMsg);
                }
            });
        } catch (Exception e) {
            ctx.response()
                    .setStatusCode(500)
                    .end("Internal server error: " + e.getMessage());
            logger.error("Internal server error: {}", e.getMessage());
        }
    }

    private void handleWaitingArea(RoutingContext ctx) {

        ctx.response()
                .putHeader("Content-Type", "application/json")
                .setStatusCode(200)
                .end(new JsonObject().put("waitingAreaData", PlayerInfo.dataWaitingArea(gsm.getPlayerInfos())).encode());
        logger.debug("Player information's successfully fetched  : {}", PlayerInfo.dataWaitingArea(gsm.getPlayerInfos()));
    }


    //Check if the request body contains the required fields
    private void handleVerifications(JsonObject json, RoutingContext ctx) {
        //Check if the request body is empty
        if (json == null || json.isEmpty()) {
            ctx.response()
                    .setStatusCode(400)
                    .end("Request body must not be empty");
            logger.error("Request body is empty");
            return;
        }

        String playerName = json.getString("playerName");//was first "name"
        String password = json.getString("password");//was first "password"

        String controllerId = json.getString("controllerId");
        logger.info("ctx.body().asString(): {}", playerName);

        if (playerName == null || playerName.isEmpty() || password == null || password.isBlank() || controllerId == null || controllerId.isBlank()) {
            ctx.response()
                    .setStatusCode(400)
                    .end("Name and password must not be empty");
            logger.error("Name or password is empty");
            return;
        }
    }


    private void getActiveController(RoutingContext ctx) {
        JsonArray jsonArray = new JsonArray(gsm.getActiveControllers());
        ctx.response()
                .putHeader("Content-Type", "application/json")
                .setStatusCode(200)
                .end(jsonArray.encode());
    }

    private void handleLogout(RoutingContext ctx) {
        String playerId = ctx.request().getParam("id");

        if (playerId == null || playerId.isBlank()) {
            ctx.response()
                    .setStatusCode(400)
                    .end("player not  log in  ");
            logger.info("player not  log in  ");
            return;
        }

        String controllerId = gsm.getControllerIdForPlayer(playerId);
        if (controllerId == null) {
            ctx.response()
                    .setStatusCode(400)
                    .end("Controller not found for playerId  : " + playerId);
            logger.info("ControllerGruppe24 not found for playerId  : " + playerId);
            return;
        }

        boolean isRemoved = gsm.removeActivePlayer(playerId);
        if (isRemoved) {
            gsm.removePlayerToController(playerId);

            ctx.response()
                    .setStatusCode(200)
                    .end("Logged out successfully");
            logger.info("Logged out successfully");
            return;

        }
        ctx.response()
                .setStatusCode(500)
                .end(" player not connected: " + playerId);
    }

    private void handleStartGame(RoutingContext ctx) {
        gs.releaseGameLock().onComplete(lockRes -> {
            if (lockRes.failed()) {
                logger.warn("Konnte Lock nicht freigeben: {}", lockRes.cause().getMessage());
                // Trotzdem fortfahren? Je nach Wunsch
            }

            gs.startGame(startRes -> {
                if (startRes.failed()) {
                    ctx.response().setStatusCode(500).end("Failed to start game");
                } else {
                    ctx.response().setStatusCode(200).end("Game is starting");
                }
            });
        });
    }


    private void handleProfileUpdate(RoutingContext ctx) {
        JsonObject jsonBody = ctx.body().asJsonObject();

        logger.debug("Received profile update request: {}", jsonBody);
        try {
            if (jsonBody == null || jsonBody.isEmpty()) {
                ctx.response().setStatusCode(400).end("Request body for profile edition must not be empty");
                logger.error("Request body for profile edition is empty");
                return;
            }
            String playerId = jsonBody.getString("id");

            String newPassword = jsonBody.getString("password");

            if (newPassword != null || playerId != null) {
                int playerIdInt = Integer.parseInt(playerId);
                playerService.updatePassword(playerIdInt, newPassword, res -> {
                    if (res.succeeded()) {
                        ctx.response().setStatusCode(200).putHeader("content-type", "application/json")
                                .end(new JsonObject().put("message", "Profile updated successfully").encode());
                        logger.debug("Profile updated successfully for playerId: {}", playerId);
                    } else {
                        String errorMsg = res.cause() != null ? res.cause().getMessage() : "Unknown error";
                        ctx.response().setStatusCode(400).putHeader("content-type", "application/json")
                                .end(new JsonObject().put("error", errorMsg).encode());
                        logger.error("Failed to update player's complete profile: {}", errorMsg);
                    }
                });
            } else {
                ctx.response().setStatusCode(400).putHeader("content-type", "application/json")
                        .end(new JsonObject().put("Error:", "New password is missing").encode());
                logger.error("New password is missing for profile update");
            }
        } catch (Exception e) {
            logger.error("Error while parsing JSON body: {} for profile update", e.getMessage());
            ctx.response().setStatusCode(400).end(new JsonObject().put("Error:", "Json body not passed!").encode());
        }
    }

    private void handleControllerDisconnect(RoutingContext ctx) {
        String body = ctx.body().asString();
        try {
            JsonObject json = new JsonObject(body);
            String controllerId = json.getString("controllerId");
            logger.info("Controller disconnected: {}", controllerId);
            gsm.removeActiveControllers(controllerId);
            JsonArray controllerIds =  new JsonArray(gsm.getActiveControllers());
            gsm.getEventBus().publish("group-24.simon.game.available.controller", new JsonObject().put("controllerIds", controllerIds));
            ctx.response().setStatusCode(200).end();
        } catch (Exception e) {
            logger.warn("Ungültige Disconnect-Anfrage", e);
            ctx.response().setStatusCode(400).end("Invalid request");
        }
    }

}