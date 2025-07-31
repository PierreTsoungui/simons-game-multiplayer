package com.example.player;

//import java.util.logging.Handler;
import com.example.game.GameStateManager;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.RowSet;
import io.vertx.core.Handler;
//import io.vertx.ext.auth.hash.BCrypt;
import org.mindrot.jbcrypt.BCrypt;

public class PlayerService {
    private final PlayerRepository playerRepository;

    PlayerService() {
        this.playerRepository = new PlayerRepository();
    }


    void createPlayer(String playerName, /*String controllerId,*/ String password, Handler<AsyncResult<Void>> resultHandler) {
        String hashPassword = BCrypt.hashpw(password, BCrypt.gensalt());

        // Delegate player creation to repository
        playerRepository.insertPlayer(playerName, hashPassword, /*controllerId,*/ resultHandler);
    }


    void playerLogin(String playerName, String password, String controllerId, GameStateManager gsm, Handler<AsyncResult<RowSet<Row>>> resultHandler) {
        playerRepository.checkPlayerInDB(playerName, password, controllerId, resultHandler, gsm);
    }



}
