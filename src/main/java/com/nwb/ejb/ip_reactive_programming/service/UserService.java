package com.nwb.ejb.ip_reactive_programming.service;

import com.nwb.ejb.ip_reactive_programming.model.User;
import com.nwb.ejb.ip_reactive_programming.repository.UserRepository;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.Promise;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class UserService {

    @Autowired
    private JDBCClient jdbcClient;

    private final UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public Future<User> save(User user) {
        Promise<User> promise = Promise.promise();
        String sql = "INSERT INTO users (name, email) VALUES (?, ?)";

        jdbcClient.getConnection(connHandler(promise, connection ->
                connection.updateWithParams(sql,
                        new io.vertx.core.json.JsonArray()
                                .add(user.getName())
                                .add(user.getEmail()), res -> {
                            connection.close();
                            if (res.succeeded()) {
                                user.setId(res.result().getKeys().getLong(0));
                                promise.complete(user);
                            } else {
                                promise.fail(res.cause());
                            }
                        })
        ));

        return promise.future();
    }

    public Future<List<User>> findAll() {
        return repository.findAll();
    }

    public Future<User> findById(Long id) {
        return repository.findById(id);
    }

    public Future<Boolean> delete(Long id) {
        return repository.delete(id);
    }

    public JDBCClient getJdbcClient() {
        return jdbcClient;
    }

    // Helper method to manage DB connections
    private <T> Handler<AsyncResult<SQLConnection>> connHandler(
            Promise<T> promise,
            Consumer<SQLConnection> handler
    ) {
        return ar -> {
            if (ar.succeeded()) {
                handler.accept(ar.result());
            } else {
                promise.fail(ar.cause());
            }
        };
    }
}
