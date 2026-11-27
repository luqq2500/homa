package com.homa.customer.usecase;

import static com.homa.customer.usecase.CustomerUsecaseFixtures.AISYAH_PERSON_ID;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.DANIEL_GOOGLE_SUBJECT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.DANIEL_PERSON_ID;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.DANIEL_SECOND_GOOGLE_SUBJECT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.HEARD_AT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.SON_GOOGLE_SUBJECT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.SON_PERSON_ID;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.VERIFIED_AT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.aisyahJustRegistered;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.aisyahsMyKad;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.customerVerified;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.registeredWithGoogle;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.verifiedAs;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.customer.domain.Customer;
import com.homa.customer.domain.PersonId;
import com.homa.customer.domain.ReservationBlocker;
import com.homa.customer.domain.VerifiedIdentity;
import com.homa.customer.usecase.ie.HandlePersonVerifiedIE;
import com.homa.customer.usecase.port.out.IdentityRefusal;
import com.homa.customer.usecase.port.out.IdentityRefusal.Reason;
import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.CustomerVerifiedIE;
import com.homa.ie.events.PersonVerifiedIE;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HandlePersonVerifiedIETest {

    private final InMemoryProcessedMessageStore processedMessages = new InMemoryProcessedMessageStore();
    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
    private final RecordingIdentityRefusals refusals = new RecordingIdentityRefusals();
    private final RecordingCustomerIEPublisher publisher = new RecordingCustomerIEPublisher();
    private final FixedTimeSource time = new FixedTimeSource(HEARD_AT);
    private final HandlePersonVerifiedIE handler = new HandlePersonVerifiedIE(processedMessages, customers, refusals,
            publisher, time, new SequentialEventIds());

    private final Customer aisyah = customers.given(aisyahJustRegistered());
    private final String aisyahsId = aisyah.id().value();

    @Test
    @Tag("CUS-3")
    @Tag("CUS-10")
    @DisplayName("Aisyah verifies with her MyKad: local and aged 18 or above, so she can reserve and Reservation "
            + "hears she is verified.")
    void aLocalAdultCustomerIsVerifiedAndAnnounced() {
        handler.handle(aisyahsMyKad("verification-1", aisyahsId));

        assertThat(customers.saves()).containsExactly(aisyah.id());
        assertThat(aisyah.verifiedIdentity())
                .contains(new VerifiedIdentity(new PersonId(AISYAH_PERSON_ID), true, true));
        assertThat(aisyah.canReserveAt(HEARD_AT)).isTrue();
        assertThat(refusals.recorded()).isEmpty();

        assertThat(publisher.published()).hasSize(1);
        CustomerVerifiedIE verified = (CustomerVerifiedIE) publisher.published().getFirst();
        assertThat(verified.eventId()).isEqualTo("event-1");
        assertThat(verified.customerId()).isEqualTo(aisyahsId);
        assertThat(verified.personId()).isEqualTo(AISYAH_PERSON_ID);
        assertThat(verified.occurredAt()).isEqualTo(HEARD_AT);
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("The relay delivers Aisyah's verification again: nothing changes and nothing is published.")
    void aRedeliveredVerificationChangesNothing() {
        PersonVerifiedIE event = aisyahsMyKad("verification-1", aisyahsId);
        handler.handle(event);

        handler.handle(event);

        assertThat(customers.saves()).containsExactly(aisyah.id());
        assertThat(publisher.published()).hasSize(1);
        assertThat(refusals.recorded()).isEmpty();
        assertThat(processedMessages.hasHandled("verification-1", HandlePersonVerifiedIE.HANDLER)).isTrue();
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("Aisyah verifies again with the same MyKad: her account records it, but she was already "
            + "announced as verified.")
    void aSecondVerificationOfTheSamePersonIsNotAnnouncedAgain() {
        handler.handle(aisyahsMyKad("verification-1", aisyahsId));

        handler.handle(aisyahsMyKad("verification-2", aisyahsId));

        assertThat(customers.saves()).containsExactly(aisyah.id(), aisyah.id());
        assertThat(publisher.published()).hasSize(1);
        assertThat(refusals.recorded()).isEmpty();
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Aisyah's 16-year-old son verifies with his MyKad and can't book; once he turns 18 and verifies "
            + "again, he can.")
    void aCustomerUnderTheMinimumAgeIsRecordedButNotAnnouncedUntilHeComesOfAge() {
        Customer son = customers.given(registeredWithGoogle(SON_GOOGLE_SUBJECT, "Aisyah's son"));
        String sonsId = son.id().value();

        handler.handle(customerVerified("verification-son-1", SON_PERSON_ID, sonsId, false));

        assertThat(customers.saves()).containsExactly(son.id());
        assertThat(son.reservationBlockerAt(HEARD_AT)).contains(ReservationBlocker.UNDER_MINIMUM_AGE);
        assertThat(publisher.published()).isEmpty();

        Instant comesOfAge = HomaFixtures.at(2028, 3, 1, 10, 0);
        time.setNow(comesOfAge);
        handler.handle(customerVerified("verification-son-2", SON_PERSON_ID, sonsId, true));

        assertThat(son.canReserveAt(comesOfAge)).isTrue();
        CustomerVerifiedIE verified = (CustomerVerifiedIE) publisher.published().getFirst();
        assertThat(verified.eventId()).isEqualTo("event-1");
        assertThat(verified.customerId()).isEqualTo(sonsId);
        assertThat(verified.personId()).isEqualTo(SON_PERSON_ID);
        assertThat(verified.occurredAt()).isEqualTo(comesOfAge);
        assertThat(refusals.recorded()).isEmpty();
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("A foreign worker verifies with her i-Kad as a customer: in Malaysia only a local may be a "
            + "customer, so she is told why she can't book.")
    void aForeignCustomerIsRecordedButNotAnnounced() {
        handler.handle(new PersonVerifiedIE("verification-1", AISYAH_PERSON_ID, "CUSTOMER", aisyahsId, "Aisyah",
                "FOREIGN", true, "2027-06-30", null, VERIFIED_AT));

        assertThat(customers.saves()).containsExactly(aisyah.id());
        assertThat(aisyah.reservationBlockerAt(HEARD_AT)).contains(ReservationBlocker.NOT_LOCAL);
        assertThat(publisher.published()).isEmpty();
        assertThat(refusals.recorded()).isEmpty();
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"OWNER", "MEMBER"})
    @DisplayName("Verifications of owners and members are Provider's: Customer ignores them.")
    void verificationsOfOwnersAndMembersAreIgnored(String role) {
        handler.handle(new PersonVerifiedIE("verification-1", AISYAH_PERSON_ID, role, aisyahsId, "Aisyah",
                "LOCAL", true, null, HomaFixtures.at(2027, 11, 30, 19, 55), VERIFIED_AT));

        assertThat(aisyah.verifiedIdentity()).isEmpty();
        assertThat(customers.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(refusals.recorded()).isEmpty();
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Daniel can't open a new account with another Google account, because his MyKad is already used.")
    void anIdentityThatHoldsAnAccountIsRefusedForASecondOne() {
        customers.given(verifiedAs(registeredWithGoogle(DANIEL_GOOGLE_SUBJECT, "Daniel"), DANIEL_PERSON_ID));
        Customer secondAccount = customers.given(registeredWithGoogle(DANIEL_SECOND_GOOGLE_SUBJECT, "Danny"));
        String secondId = secondAccount.id().value();
        PersonVerifiedIE event = customerVerified("verification-daniel-2", DANIEL_PERSON_ID, secondId, true);

        handler.handle(event);

        assertThat(secondAccount.verifiedIdentity()).isEmpty();
        assertThat(customers.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(refusals.recorded()).containsExactly(new IdentityRefusal("verification-daniel-2", secondId,
                DANIEL_PERSON_ID, Reason.IDENTITY_HOLDS_ANOTHER_ACCOUNT, HEARD_AT));

        handler.handle(event);

        assertThat(refusals.recorded()).hasSize(1);
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Aisyah's account, verified with her MyKad, can't then be verified as Daniel.")
    void anAccountVerifiedAsOnePersonIsRefusedAnother() {
        verifiedAs(aisyah, AISYAH_PERSON_ID);

        handler.handle(customerVerified("verification-2", DANIEL_PERSON_ID, aisyahsId, true));

        assertThat(aisyah.verifiedIdentity().orElseThrow().personId().value()).isEqualTo(AISYAH_PERSON_ID);
        assertThat(customers.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(refusals.recorded()).containsExactly(new IdentityRefusal("verification-2", aisyahsId,
                DANIEL_PERSON_ID, Reason.ACCOUNT_HOLDS_ANOTHER_IDENTITY, HEARD_AT));
    }

    @Test
    @DisplayName("A customer verification for an account Customer doesn't have is recorded as refused, not "
            + "retried.")
    void aVerificationForAnUnknownAccountIsRefused() {
        handler.handle(aisyahsMyKad("verification-1", "customer-unknown"));

        assertThat(customers.saves()).isEmpty();
        assertThat(publisher.published()).isEmpty();
        assertThat(refusals.recorded()).containsExactly(new IdentityRefusal("verification-1", "customer-unknown",
                AISYAH_PERSON_ID, Reason.ACCOUNT_NOT_FOUND, HEARD_AT));
    }

    @ParameterizedTest(name = "accountId \"{0}\"")
    @ValueSource(strings = {"", " "})
    @DisplayName("A customer verification that names no account is recorded as refused, not retried.")
    void aVerificationNamingNoAccountIsRefused(String accountId) {
        handler.handle(aisyahsMyKad("verification-1", accountId));

        assertThat(refusals.recorded()).extracting(IdentityRefusal::reason).containsExactly(Reason.ACCOUNT_NOT_FOUND);
        assertThat(customers.saves()).isEmpty();
    }

    @Test
    @DisplayName("A customer verification with no account id at all is recorded as refused, not retried.")
    void aVerificationWithoutAnAccountIdIsRefused() {
        handler.handle(aisyahsMyKad("verification-1", null));

        assertThat(refusals.recorded()).containsExactly(new IdentityRefusal("verification-1", null,
                AISYAH_PERSON_ID, Reason.ACCOUNT_NOT_FOUND, HEARD_AT));
        assertThat(aisyah.verifiedIdentity()).isEmpty();
    }
}
