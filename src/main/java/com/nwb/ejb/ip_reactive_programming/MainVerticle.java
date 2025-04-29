package com.nwb.ejb.ip_reactive_programming;

import com.nwb.ejb.ip_reactive_programming.model.User;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.json.DecodeException;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.BodyHandler;


public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Promise<Void> startPromise) {
        JDBCClient jdbc = JDBCClient.createShared(vertx, new JsonObject()
                .put("url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
                .put("driver_class", "org.h2.Driver")
                .put("user", "sa"));

        jdbc.getConnection(ar -> {
            if (ar.succeeded()) {
                SQLConnection conn = ar.result();
                conn.execute("CREATE TABLE IF NOT EXISTS \"USER\" (id IDENTITY PRIMARY KEY, NAME VARCHAR, EMAIL VARCHAR)\n", res -> {
                    conn.close();
                    if (res.succeeded()) {
                        System.out.println("==================S======================>");
                        setupRoutes(jdbc, startPromise);
                    } else {
                        startPromise.fail(res.cause());
                        System.out.println("================F========================>");
                    }
                });
            } else {

                startPromise.fail(ar.cause());
            }
        });
    }

    private void setupRoutes(JDBCClient jdbc, Promise<Void> startPromise) {
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
/*        router.post("/api/users").handler(ctx -> {
            System.out.println("==========>"+ctx.getBodyAsString());
            User user = Json.decodeValue(ctx.getBodyAsString(), User.class);
            jdbc.updateWithParams("INSERT INTO user (name, email) VALUES (?, ?)",
                    new JsonArray().add(user.name).add(user.email), res -> {
                        if (res.succeeded()) {
                            ctx.response().setStatusCode(201).end();
                        } else {
                            ctx.response().setStatusCode(500).end("Failed to insert user");
                        }
                    });
        });*/


        router.post("/api/users").handler(ctx -> {
            // Offload JSON parsing and DB logic to a worker thread
            vertx.executeBlocking(promise -> {
                try {
                    // Step 1: Parse JSON
                    String jsonBody = ctx.getBodyAsString();
                    User user = Json.decodeValue(jsonBody, User.class);

                    // Step 2: Execute async DB operation
                    jdbc.updateWithParams(
                            "INSERT INTO \"USER\" (name, email) VALUES (?, ?)",
                            new JsonArray().add(user.name).add(user.email),
                            dbRes -> {
                                if (dbRes.succeeded()) {
                                    promise.complete();
                                } else {
                                    promise.fail(dbRes.cause());
                                }
                            }
                    );
                } catch (DecodeException e) {
                    promise.fail("Invalid JSON: " + e.getMessage());
                } catch (Exception e) {
                    promise.fail("Unexpected error: " + e.getMessage());
                }
            }, false, res -> { // 'false' for non-ordered execution
                if (res.succeeded()) {
                    ctx.response().setStatusCode(201).end();
                } else {
                    ctx.response()
                            .setStatusCode(500)
                            .end(res.cause().getMessage());
                }
            });
        });

        router.get("/api/users").handler(ctx -> {
            jdbc.query("SELECT * FROM user", res -> {
                if (res.succeeded()) {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(Json.encodePrettily(res.result().getRows()));
                } else {
                    ctx.response().setStatusCode(500).end("Failed to fetch users");
                }
            });
        });

        // Get User by ID
        router.get("/api/users/:id").handler(ctx -> {
            Long id = Long.valueOf(ctx.pathParam("id"));
            jdbc.queryWithParams("SELECT * FROM user WHERE id = ?", new JsonArray().add(id), res -> {
                if (res.succeeded() && !res.result().getRows().isEmpty()) {
                    ctx.response().putHeader("Content-Type", "application/json")
                            .end(Json.encodePrettily(res.result().getRows().get(0)));
                } else {
                    ctx.response().setStatusCode(404).end("User not found");
                }
            });
        });

        // Update User
        router.put("/api/users/:id").handler(ctx -> {
            Long id = Long.valueOf(ctx.pathParam("id"));
            User user = Json.decodeValue(ctx.getBodyAsString(), User.class);
            jdbc.updateWithParams("UPDATE user SET name = ?, email = ? WHERE id = ?",
                    new JsonArray().add(user.name).add(user.email).add(id), res -> {
                        if (res.succeeded()) {
                            ctx.response().setStatusCode(204).end();
                        } else {
                            ctx.response().setStatusCode(500).end("Failed to update user");
                        }
                    });
        });

        router.delete("/api/vertices/:id").handler(ctx -> {
            Long id = Long.valueOf(ctx.pathParam("id"));
            jdbc.updateWithParams("DELETE FROM user WHERE id = ?", new JsonArray().add(id),
                    res -> ctx.response().setStatusCode(204).end());
        });

        vertx.createHttpServer().requestHandler(router).listen(8888, http -> {
            if (http.succeeded()) {
                startPromise.complete();
                System.out.println("Vert.x HTTP server running at http://localhost:8888");
            } else {
                startPromise.fail(http.cause());
            }
        });
    }
}

