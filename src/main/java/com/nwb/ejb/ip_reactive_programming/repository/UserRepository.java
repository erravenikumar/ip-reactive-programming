package com.nwb.ejb.ip_reactive_programming.repository;
import com.nwb.ejb.ip_reactive_programming.model.User;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.sql.SQLClient;
import io.vertx.ext.sql.ResultSet;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
public class UserRepository {

    private final SQLClient client;

    public UserRepository(SQLClient client) {
        this.client = client;
    }

    public Future<User> save(User user) {
        Promise<User> promise = Promise.promise();
        String query = "INSERT INTO users (name, email) VALUES (?, ?) RETURNING id";
        client.queryWithParams(query, new JsonArray().add(user.getName()).add(user.getEmail()), res -> {
            if (res.succeeded()) {
                long id = res.result().getRows().get(0).getLong("id");
                user.setId(id);
                promise.complete(user);
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    public Future<List<User>> findAll() {
        Promise<List<User>> promise = Promise.promise();
        client.query("SELECT id AS \"id\", name AS \"name\", email AS \"email\" FROM users", res -> {
            if (res.succeeded()) {
                List<User> users = res.result().getRows().stream().map(row -> {
                    System.out.println("Row from DB: " + row.encodePrettily());

                    User user = new User();
                    user.setId(((Number) row.getValue("id")).longValue());
                    user.setName((String) row.getValue("name"));
                    user.setEmail((String) row.getValue("email"));
                    return user;
                }).collect(Collectors.toList());
                promise.complete(users);
            } else {
                promise.fail(res.cause());
            }
        });
        return promise.future();
    }

    public Future<User> findById(Long id) {
        Promise<User> promise = Promise.promise();
        client.queryWithParams("SELECT id AS \"id\", name AS \"name\", email AS \"email\" FROM users WHERE id = ?", new JsonArray().add(id), res -> {
            if (res.succeeded() && !res.result().getRows().isEmpty()) {
                var row = res.result().getRows().get(0);
                User user = new User();
                user.setId(row.getLong("id"));
                user.setName(row.getString("name"));
                user.setEmail(row.getString("email"));
                promise.complete(user);
            } else {
                promise.fail("User not found");
            }
        });
        return promise.future();
    }

    public Future<Boolean> delete(Long id) {
        Promise<Boolean> promise = Promise.promise();
        client.updateWithParams("DELETE FROM users WHERE id = ?", new JsonArray().add(id), res -> {
            if (res.succeeded() && res.result().getUpdated() > 0) {
                promise.complete(true);
            } else {
                promise.fail("User not found or already deleted");
            }
        });
        return promise.future();
    }
}
