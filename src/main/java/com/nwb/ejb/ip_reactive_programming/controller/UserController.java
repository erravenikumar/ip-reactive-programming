package com.nwb.ejb.ip_reactive_programming.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nwb.ejb.ip_reactive_programming.model.User;
import com.nwb.ejb.ip_reactive_programming.service.UserService;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import io.vertx.core.Future;
import io.vertx.core.Promise;


@Component
public class UserController extends AbstractVerticle {

    @Autowired
    private UserService service;

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public void start() {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());

        createUsersTable().onSuccess(v -> {
            setupRoutes(router);
            vertx.createHttpServer()
                    .requestHandler(router)
                    .listen(8080)
                    .onSuccess(server ->
                            System.out.println("HTTP server started on port " + server.actualPort()));
        }).onFailure(err -> {
            System.err.println("Failed to initialize DB: " + err.getMessage());
        });
    }

    private Future<Void> createUsersTable() {
        Promise<Void> promise = Promise.promise();

        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                name VARCHAR(255) NOT NULL,
                email VARCHAR(255) NOT NULL
            );
        """;

        service.getJdbcClient().query(sql, ar -> {
            if (ar.succeeded()) {
                promise.complete();
            } else {
                promise.fail(ar.cause());
            }
        });

        return promise.future();
    }

    private void setupRoutes(Router router) {

        // Get all users
        router.get("/users").handler(ctx ->
                service.findAll().onSuccess(users ->
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end(Json.encode(users))
                ).onFailure(err ->
                        ctx.response().setStatusCode(500).end(err.getMessage()))
        );

        // Get user by ID
        router.get("/users/:id").handler(ctx -> {
            Long id = Long.valueOf(ctx.pathParam("id"));
            service.findById(id).onSuccess(user -> {
                vertx.executeBlocking(p -> {
                    try {
                        p.complete(mapper.writeValueAsString(user));
                    } catch (Exception e) {
                        p.fail(e);
                    }
                }, jsonRes -> {
                    if (jsonRes.succeeded()) {
                        ctx.response()
                                .putHeader("Content-Type", "application/json")
                                .end((String) jsonRes.result());
                    } else {
                        ctx.fail(jsonRes.cause());
                    }
                });
            }).onFailure(err ->
                    ctx.response().setStatusCode(404).end(err.getMessage()));
        });

        // Create user
        router.post("/users").handler(ctx -> {
            vertx.executeBlocking(p -> {
                try {
                    User user = mapper.readValue(ctx.getBodyAsString(), User.class);
                    p.complete(user);
                } catch (Exception e) {
                    p.fail(e);
                }
            }, parseRes -> {
                if (parseRes.succeeded()) {
                    User user = (User) parseRes.result();
                    service.save(user).onSuccess(saved -> {
                        vertx.executeBlocking(jsonP -> {
                            try {
                                jsonP.complete(mapper.writeValueAsString(saved));
                            } catch (Exception e) {
                                jsonP.fail(e);
                            }
                        }, jsonRes -> {
                            if (jsonRes.succeeded()) {
                                ctx.response()
                                        .setStatusCode(201)
                                        .putHeader("Content-Type", "application/json")
                                        .end((String) jsonRes.result());
                            } else {
                                ctx.fail(jsonRes.cause());
                            }
                        });
                    }).onFailure(ctx::fail);
                } else {
                    ctx.fail(parseRes.cause());
                }
            });
        });

        // Delete user
        router.delete("/users/:id").handler(ctx -> {
            Long id = Long.valueOf(ctx.pathParam("id"));
            service.delete(id).onSuccess(deleted -> {
                if (deleted) {
                    ctx.response().setStatusCode(204).end();
                } else {
                    ctx.response().setStatusCode(404).end("User not found");
                }
            }).onFailure(err ->
                    ctx.response().setStatusCode(500).end(err.getMessage()));
        });
    }
}
