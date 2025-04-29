package com.nwb.ejb.ip_reactive_programming.config;


import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VertxConfig {

    @Bean
    public JDBCClient jdbcClient(Vertx vertx) {
        JsonObject config = new io.vertx.core.json.JsonObject()
                .put("url", "jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1")
                .put("driver_class", "org.h2.Driver")
                .put("user", "sa")
                .put("password", "");
        return JDBCClient.createShared(vertx, config);
    }
}
