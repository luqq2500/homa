package com.homa.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The one deployable and composition root. Only this package may see every bounded context: it wires each
 * context's infra to its usecase ports and wraps usecases in transaction decorators, so usecase code carries no
 * Spring annotation.
 */
@SpringBootApplication(scanBasePackages = "com.homa")
public class HomaApplication {

    public static void main(String[] args) {
        SpringApplication.run(HomaApplication.class, args);
    }
}
