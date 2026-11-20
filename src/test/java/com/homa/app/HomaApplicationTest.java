package com.homa.app;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(classes = HomaApplication.class)
class HomaApplicationTest {

    @Test
    void the_application_starts(ApplicationContext context) {
        assertThat(context).isNotNull();
    }
}
