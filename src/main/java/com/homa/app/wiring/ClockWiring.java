package com.homa.app.wiring;

import com.homa.kernel.BusinessCalendar;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The one clock every context reads through its {@code TimeSource} adapter: the system clock in Homa's business zone,
 * Asia/Kuala_Lumpur (plan, "Time-driven rules enter through schedulers"). Tests replace this bean with a fixed clock.
 */
@Configuration(proxyBeanMethods = false)
public class ClockWiring {

    @Bean
    public Clock clock() {
        return Clock.system(BusinessCalendar.ZONE);
    }
}
