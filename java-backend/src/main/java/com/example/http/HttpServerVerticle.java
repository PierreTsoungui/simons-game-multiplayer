package com.example.http;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CorsHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

public class HttpServerVerticle extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(HttpServerVerticle.class);

    public Router router = Router.router(vertx);

    @Override
    public void start() {
        // Configure Router
        router.route().handler(BodyHandler.create());


        //router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));
        //router.route().handler(sessionHandler);
        // CORS Configuration
        Set<String> allowedHeaders = Set.of(
            "x-requested-with",
            "Access-Control-Allow-Origin",
            "origin",
            "Content-Type",
            "accept"
        );
        router.route().handler(CorsHandler.create().allowedHeaders(allowedHeaders)
                    .allowedMethod(io.vertx.core.http.HttpMethod.PATCH)
                    .allowedMethod(io.vertx.core.http.HttpMethod.PUT)
                    .allowedMethod(io.vertx.core.http.HttpMethod.GET)
                   .allowedMethod(io.vertx.core.http.HttpMethod.POST)
        );
        
        vertx.createHttpServer()
            .requestHandler(router)
            .listen(8080, "0.0.0.0",http -> {
                if (http.succeeded()) {
                    logger.info("HTTP server started on port 8080");
                } else {
                    logger.error("Failed to start HTTP server: {}", http.cause().getMessage());
                }
            });
    }
}
