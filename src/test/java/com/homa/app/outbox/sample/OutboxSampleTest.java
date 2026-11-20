package com.homa.app.outbox.sample;

import com.homa.app.HomaApplication;
import com.homa.app.PostgresContainer;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * The application with the sample contexts, on its own PostgreSQL. The scheduled relay is off, so each test runs the
 * relay itself and decides when delivery happens. Every test using this shares one application context.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = HomaApplication.class, properties = {
        "homa.outbox.relay.enabled=false",
        "spring.flyway.locations=classpath:db/migration,classpath:db/sample"})
@Import({PostgresContainer.class, SampleContexts.class})
public @interface OutboxSampleTest {
}
