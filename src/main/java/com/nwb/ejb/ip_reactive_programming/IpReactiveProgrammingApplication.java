package com.nwb.ejb.ip_reactive_programming;

import io.vertx.core.Vertx;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class IpReactiveProgrammingApplication {

	public static void main(String[] args) {
		SpringApplication.run(IpReactiveProgrammingApplication.class, args);
	}
	@Bean
	public CommandLineRunner deployVerticle(Vertx vertx) {
		System.out.println("========================================>");
		return args -> vertx.deployVerticle(new MainVerticle());
	}

	@Bean
	public Vertx vertx() {
		return Vertx.vertx();
	}
}
