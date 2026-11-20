package com.homa.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * The one deployable and composition root. Only this package may see every bounded context: it wires each
 * context's infra to its usecase ports and wraps usecases in transaction decorators, so usecase code carries no
 * Spring annotation. It runs as a long-running container on ECS/Fargate (docs/decisions/0001), so the outbox relay
 * and the schedulers run in process.
 */
@SpringBootApplication(scanBasePackages = "com.homa")
@EnableScheduling
public class HomaApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomaApplication.class, args);
    }
}
