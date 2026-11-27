package com.homa.customer.usecase;

import static com.homa.customer.usecase.CustomerUsecaseFixtures.AISYAH_PERSON_ID;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.HEARD_AT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.REGISTERED_AT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.aisyahRegisters;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.aisyahsMyKad;
import static com.homa.fixtures.HomaFixtures.AISYAH_CITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.customer.domain.Customer;
import com.homa.customer.domain.exception.SignInAlreadyHoldsAccountException;
import com.homa.customer.usecase.dto.RegisterAccountResponse;
import com.homa.customer.usecase.ie.HandlePersonVerifiedIE;
import com.homa.ie.events.CustomerRegisteredIE;
import com.homa.ie.events.CustomerVerifiedIE;
import com.homa.ie.events.PersonVerifiedIE;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * Flow A from the Customer side: Aisyah signs in with Google, registers, and verifies with her MyKad when she first
 * taps Reserve, before paying. Reservation learns she can reserve from exactly one {@link CustomerVerifiedIE}, even
 * though the relay delivers Verification's event twice.
 */
class CustomerFlowATest {

    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
    private final RecordingCustomerIEPublisher publisher = new RecordingCustomerIEPublisher();
    private final RecordingIdentityRefusals refusals = new RecordingIdentityRefusals();
    private final FixedTimeSource time = new FixedTimeSource(REGISTERED_AT);
    private final SequentialEventIds eventIds = new SequentialEventIds();

    private final CustomerRegisterAccount registerAccount =
            new CustomerRegisterAccount(customers, publisher, time, eventIds);
    private final HandlePersonVerifiedIE personVerified = new HandlePersonVerifiedIE(
            new InMemoryProcessedMessageStore(), customers, refusals, publisher, time, eventIds);

    @Test
    @Tag("CUS-1")
    @Tag("CUS-2")
    @Tag("CUS-3")
    @Tag("CUS-7")
    @Tag("CUS-10")
    @DisplayName("Aisyah signs in with Google, registers, verifies with her MyKad, and is announced verified once.")
    void aisyahRegistersAndVerifiesBeforeHerFirstReservation() {
        RegisterAccountResponse registered = registerAccount.register(aisyahRegisters());
        String aisyahsId = registered.customerId();
        Customer aisyah = customers.get(aisyahsId);
        assertThat(aisyah.canReserveAt(REGISTERED_AT)).isFalse();

        assertThatThrownBy(() -> registerAccount.register(aisyahRegisters()))
                .isInstanceOf(SignInAlreadyHoldsAccountException.class);

        time.setNow(HEARD_AT);
        PersonVerifiedIE myKad = aisyahsMyKad("verification-aisyah-1", aisyahsId);
        personVerified.handle(myKad);
        personVerified.handle(myKad);

        assertThat(aisyah.canReserveAt(HEARD_AT)).isTrue();
        assertThat(refusals.recorded()).isEmpty();
        assertThat(publisher.published()).hasSize(2);

        CustomerRegisteredIE registeredEvent = (CustomerRegisteredIE) publisher.published().get(0);
        assertThat(registeredEvent.eventId()).isEqualTo("event-1");
        assertThat(registeredEvent.customerId()).isEqualTo(aisyahsId);
        assertThat(registeredEvent.city()).isEqualTo(AISYAH_CITY);
        assertThat(registeredEvent.streetAddress()).isEqualTo(CustomerUsecaseFixtures.AISYAH_STREET_ADDRESS);
        assertThat(registeredEvent.arrivalNote()).isEqualTo(CustomerUsecaseFixtures.AISYAH_ARRIVAL_NOTE);
        assertThat(registeredEvent.status()).isEqualTo("ACTIVE");
        assertThat(registeredEvent.occurredAt()).isEqualTo(REGISTERED_AT);

        CustomerVerifiedIE verifiedEvent = (CustomerVerifiedIE) publisher.published().get(1);
        assertThat(verifiedEvent.eventId()).isEqualTo("event-2");
        assertThat(verifiedEvent.customerId()).isEqualTo(aisyahsId);
        assertThat(verifiedEvent.personId()).isEqualTo(AISYAH_PERSON_ID);
        assertThat(verifiedEvent.occurredAt()).isEqualTo(HEARD_AT);
    }
}
