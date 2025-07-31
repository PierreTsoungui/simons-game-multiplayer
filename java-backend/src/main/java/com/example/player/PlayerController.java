package com.example.player;

import com.example.game.GameStateManager;
import com.example.http.HttpController;
import com.example.object.ObjectController;
import com.example.object.ObjectService;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.mindrot.jbcrypt.BCrypt;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collector;

public class PlayerController implements HttpController {

    private static final Logger logger = LoggerFactory.getLogger(PlayerController.class);
    private final PlayerService playerService;
    private final EventBus eventBus;
    private GameStateManager gsm;
    private PlayerInfo playerInfo;
    public PlayerController(Vertx vertx) {
        this.eventBus = vertx.eventBus();
        this.playerService = new PlayerService();
        this.gsm= GameStateManager.getInstance(vertx);
    }

    public void registerRoutes(Router router) {
        router.post("/api/players/register").handler(this::handleCreate);
        router.post("/api/players/login").handler(this::handleLogin);
        router.get("/api/waitingArea").handler(this::handleWaitingArea);

       /* router.post("/api/logout").handler(this.handlerLogout);
        router.delete("/api/player/:id").handler(this.HandlerDelete);
        router.patch("/api/player/:id").handler(this.HandlerUpdate);*/


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
            if (userName == null ||userName.isBlank() || password == null  || password.isBlank() ) {
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

            //Check if the request body contains the required fields
            try{
                handleVerifications(json, ctx);

                String playerName = json.getString("playerName");//was first "name"
                String password = json.getString("password");//was first "password"

                String controllerId = json.getString("controllerId");// comes here as web controller's id from frontend

                playerService.playerLogin(playerName, password, controllerId ,gsm ,res -> {
                    if (res.succeeded()) {
                        ctx.response().setStatusCode(201).putHeader("content-type", "application/json")
                                .end(new JsonObject().put("message", "Login successful").encode());
                        logger.debug("Login successful");
                        logger.info("Event published: player.logged in with name: {}", playerName);
                    }else {
                        String errorMsg = res.cause() != null ? res.cause().getMessage() : "Unknown error";
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .setStatusCode(400)
                                .end(new JsonObject().put("error", errorMsg).encode());
                        logger.error("Failed to login player : {}", errorMsg);
                    }
                });
            }catch (Exception e) {
                ctx.response()
                        .setStatusCode(500)
                        .end("Internal server error: " + e.getMessage());
                logger.error("Internal server error: {}", e.getMessage());
            }
        }
    private void handleWaitingArea(RoutingContext ctx) {
        JsonObject json = new JsonObject();

        ctx.response()
                .putHeader("Content-Type", "application/json")
                .setStatusCode(200)
                .end(new JsonObject().put("waitingAreaData", playerInfo.dataWaitingArea(gsm.getPlayerInfos())).encode());
        logger.debug("Player informations successfully fetched  : {}", playerInfo.dataWaitingArea(gsm.getPlayerInfos()));
    }


   /* void isCorrectdaten(JsonObject jsonBody, RoutingContext ctx) {

        if (jsonBody == null) {
            ctx.response().setStatusCode(400).end("Request body must be JSON");
            return;
        }

        String userName = jsonBody.getString("userName");
        String password = jsonBody.getString("password");
        if (userName == null ||userName.isBlank() || password == null  || password.isBlank() ) {
            ctx.response().setStatusCode(400).end("Username or password is missing");
            return;
        }


    }*/

    public void checkJsonformat(RoutingContext ctx) {
      JsonObject jsonBody = ctx.body().asJsonObject();



    }

    private void handleVerifications(JsonObject json, RoutingContext ctx){
        //Check if the request body is empty
        if(json == null || json.isEmpty()) {
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

        if(playerName == null || playerName.isEmpty() || password == null || password.isBlank() || controllerId == null || controllerId.isBlank()) {
            ctx.response()
                    .setStatusCode(400)
                    .end("Name and password must not be empty");
            logger.error("Name or password is empty");
            return;
        }
    }



}