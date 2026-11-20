package com.homa.app.outbox.sample;

import com.homa.app.outbox.sample.conversation.infra.event.SpringConversationIEListener;
import com.homa.app.outbox.sample.conversation.infra.persistent.JdbcMembers;
import com.homa.app.outbox.sample.registration.infra.event.SpringRegistrationIEListener;
import com.homa.app.outbox.sample.registration.infra.event.SpringRegistrationIEPublisher;
import com.homa.app.outbox.sample.registration.infra.persistent.JdbcCustomers;
import com.homa.app.outbox.sample.registration.usecase.RegisterCustomer;
import com.homa.app.outbox.sample.standing.infra.event.SpringStandingIEListener;
import com.homa.app.outbox.sample.standing.infra.persistent.JdbcCustomerStandings;
import com.homa.app.outbox.sample.standing.infra.persistent.JdbcProcessedMessageStore;
import com.homa.app.outbox.sample.standing.usecase.ie.HandleCustomerRegisteredIE;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Test only: wiring for three sample contexts, the way com.homa.app will wire real ones. "registration" publishes
 * CustomerRegisteredIE; "standing" and "conversation" each consume it through the outbox with their own
 * ProcessedMessageStore.
 */
@TestConfiguration(proxyBeanMethods = false)
public class SampleContexts {

    @Bean
    DeliveryLog deliveryLog() {
        return new DeliveryLog();
    }

    @Bean
    FailureSwitch failureSwitch() {
        return new FailureSwitch();
    }

    @Bean
    RegisterCustomer registerCustomer(JdbcClient jdbc, ApplicationEventPublisher events) {
        return new RegisterCustomer(new JdbcCustomers(jdbc), new SpringRegistrationIEPublisher(events));
    }

    @Bean
    SpringRegistrationIEListener registrationListener(DeliveryLog log) {
        return new SpringRegistrationIEListener(log);
    }

    @Bean
    SpringStandingIEListener standingListener(JdbcClient jdbc, FailureSwitch failures, DeliveryLog log) {
        return new SpringStandingIEListener(new HandleCustomerRegisteredIE(new JdbcProcessedMessageStore(jdbc),
                new JdbcCustomerStandings(jdbc, failures)), log);
    }

    @Bean
    SpringConversationIEListener conversationListener(JdbcClient jdbc, FailureSwitch failures, DeliveryLog log) {
        return new SpringConversationIEListener(
                new com.homa.app.outbox.sample.conversation.usecase.ie.HandleCustomerRegisteredIE(
                        new com.homa.app.outbox.sample.conversation.infra.persistent.JdbcProcessedMessageStore(jdbc),
                        new JdbcMembers(jdbc, failures)),
                log);
    }
}
