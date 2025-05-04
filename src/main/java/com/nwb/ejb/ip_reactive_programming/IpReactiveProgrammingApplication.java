package com.nwb.ejb.ip_reactive_programming;

import com.nwb.ejb.ip_reactive_programming.controller.UserController;
import io.vertx.core.Vertx;
import jakarta.annotation.PostConstruct;
import org.apache.commons.logging.Log;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class IpReactiveProgrammingApplication {
	private final Vertx vertx;
	private final UserController userController;

	public IpReactiveProgrammingApplication(Vertx vertx, UserController userController) {
		this.vertx = vertx;
		this.userController = userController;
	}
	public static void main(String[] args) {
		ApplicationContext context =SpringApplication.run(IpReactiveProgrammingApplication.class, args);

	}
	@PostConstruct
	public void deployVerticle() {
		vertx.executeBlocking(promise -> {
			vertx.deployVerticle(userController, ar -> {
				if (ar.succeeded()) {
				System.out.println("UserController deployed with ID: " + ar.result());
					promise.complete();
				} else {
					promise.fail(ar.cause());
				}
			});
		}, res -> {
			if (res.failed()) {
				System.err.println("Failed to deploy Verticle: " + res.cause());
			}
		});
	}

}
