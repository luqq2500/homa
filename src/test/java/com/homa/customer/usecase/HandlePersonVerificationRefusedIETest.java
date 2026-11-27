package com.homa.customer.usecase;

import static com.homa.customer.usecase.CustomerUsecaseFixtures.AISYAH_PERSON_ID;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.DANIEL_PERSON_ID;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.DANIEL_SECOND_GOOGLE_SUBJECT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.HEARD_AT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.SON_GOOGLE_SUBJECT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.SON_PERSON_ID;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.VERIFIED_AT;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.aisyahJustRegistered;
import static com.homa.customer.usecase.CustomerUsecaseFixtures.registeredWithGoogle;
import static org.assertj.core.api.Assertions.assertThat;

import com.homa.customer.domain.Customer;
import com.homa.customer.domain.ReservationBlocker;
import com.homa.customer.domain.VerificationRefusal;
import com.homa.customer.usecase.ie.HandlePersonVerificationRefusedIE;
import com.homa.customer.usecase.port.out.IdentityRefusal;
import com.homa.customer.usecase.port.out.IdentityRefusal.Reason;
import com.homa.ie.events.PersonVerificationRefusedIE;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class HandlePersonVerificationRefusedIETest {

    private final InMemoryProcessedMessageStore processedMessages = new InMemoryProcessedMessageStore();
    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
    private final RecordingIdentityRefusals refusals = new RecordingIdentityRefusals();
    private final HandlePersonVerificationRefusedIE handler = new HandlePersonVerificationRefusedIE(processedMessages,
            customers, refusals, new FixedTimeSource(HEARD_AT));

    private static PersonVerificationRefusedIE refused(String eventId, String personId, String role, String accountId,
            String reason, String rule) {
        return new PersonVerificationRefusedIE(eventId, personId, role, accountId, reason, rule, VERIFIED_AT);
    }

    private static PersonVerificationRefusedIE customerRefused(String eventId, String personId, String accountId,
            String reason) {
        return refused(eventId, personId, "CUSTOMER", accountId, reason, "CUS-10");
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Aisyah's 16-year-old son can't book: he's told he's under the minimum age, not asked to verify "
            + "again.")
    void anUnderageCustomerIsToldWhy() {
        Customer son = customers.given(registeredWithGoogle(SON_GOOGLE_SUBJECT, "Aisyah's son"));

        handler.handle(customerRefused("refusal-1", SON_PERSON_ID, son.id().value(), "UNDER_MINIMUM_AGE"));

        assertThat(customers.saves()).containsExactly(son.id());
        assertThat(son.verificationRefusal()).contains(VerificationRefusal.UNDER_MINIMUM_AGE);
        assertThat(son.reservationBlockerAt(HEARD_AT)).contains(ReservationBlocker.UNDER_MINIMUM_AGE);
        assertThat(son.verifiedIdentity()).isEmpty();
        assertThat(refusals.recorded()).isEmpty();
        assertThat(processedMessages.hasHandled("refusal-1", "customer.PersonVerificationRefused")).isTrue();
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("A foreign visitor can't be a customer in Malaysia: they're told their status doesn't allow it.")
    void aForeignVisitorIsToldWhy() {
        Customer visitor = customers.given(registeredWithGoogle("visitor-google-subject", "Visitor"));

        handler.handle(customerRefused("refusal-1", "person-visitor", visitor.id().value(),
                "STATUS_NOT_ALLOWED_FOR_ROLE"));

        assertThat(customers.saves()).containsExactly(visitor.id());
        assertThat(visitor.reservationBlockerAt(HEARD_AT)).contains(ReservationBlocker.NOT_LOCAL);
        assertThat(refusals.recorded()).isEmpty();
    }

    @Test
    @Tag("CUS-10")
    @Tag("IDN-3")
    @DisplayName("Daniel's second account can't reserve: it's told his MyKad holds another account, and the refusal "
            + "is kept for Support.")
    void aSecondAccountIsToldTheIdentityIsHeldAndTheRefusalKept() {
        Customer second = customers.given(registeredWithGoogle(DANIEL_SECOND_GOOGLE_SUBJECT, "Daniel"));
        String secondId = second.id().value();

        handler.handle(refused("refusal-1", DANIEL_PERSON_ID, "CUSTOMER", secondId, "IDENTITY_ALREADY_HELD",
                "IDN-3"));

        assertThat(customers.saves()).containsExactly(second.id());
        assertThat(second.reservationBlockerAt(HEARD_AT)).contains(ReservationBlocker.IDENTITY_HELD_BY_ANOTHER_ACCOUNT);
        assertThat(refusals.recorded()).containsExactly(new IdentityRefusal("refusal-1", secondId, DANIEL_PERSON_ID,
                Reason.IDENTITY_HOLDS_ANOTHER_ACCOUNT, HEARD_AT));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DOCUMENT_NOT_ACCEPTED", "CHECK_NOT_PASSED"})
    @Tag("IDN-1")
    @DisplayName("A document not accepted, or a check not passed, changes nothing: Aisyah is asked to verify again.")
    void aRefusalTheCustomerCanRetryChangesNothing(String reason) {
        Customer aisyah = customers.given(aisyahJustRegistered());

        handler.handle(refused("refusal-1", AISYAH_PERSON_ID, "CUSTOMER", aisyah.id().value(), reason, "IDN-1"));

        assertThat(customers.saves()).isEmpty();
        assertThat(aisyah.verificationRefusal()).isEmpty();
        assertThat(aisyah.reservationBlockerAt(HEARD_AT)).contains(ReservationBlocker.IDENTITY_NOT_VERIFIED);
        assertThat(refusals.recorded()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"OWNER", "MEMBER"})
    @Tag("PRV-1")
    @DisplayName("An owner's or a member's refusal is Provider's: Customer ignores it.")
    void anOwnersOrMembersRefusalIsIgnored(String role) {
        Customer aisyah = customers.given(aisyahJustRegistered());

        handler.handle(refused("refusal-1", AISYAH_PERSON_ID, role, aisyah.id().value(), "UNDER_MINIMUM_AGE",
                "PRV-1"));

        assertThat(customers.saves()).isEmpty();
        assertThat(aisyah.verificationRefusal()).isEmpty();
        assertThat(refusals.recorded()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"customer-unknown", " "})
    @DisplayName("A refusal for an account Customer doesn't have is kept for Support and changes no account.")
    void aRefusalForAnUnknownAccountIsKept(String accountId) {
        handler.handle(customerRefused("refusal-1", SON_PERSON_ID, accountId, "UNDER_MINIMUM_AGE"));

        assertThat(customers.saves()).isEmpty();
        assertThat(refusals.recorded()).containsExactly(new IdentityRefusal("refusal-1", accountId, SON_PERSON_ID,
                Reason.ACCOUNT_NOT_FOUND, HEARD_AT));
    }

    @Test
    @DisplayName("A refusal naming no account is kept for Support.")
    void aRefusalNamingNoAccountIsKept() {
        handler.handle(customerRefused("refusal-1", SON_PERSON_ID, null, "UNDER_MINIMUM_AGE"));

        assertThat(refusals.recorded()).containsExactly(new IdentityRefusal("refusal-1", null, SON_PERSON_ID,
                Reason.ACCOUNT_NOT_FOUND, HEARD_AT));
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("The relay delivers the son's refusal again: nothing changes.")
    void aRedeliveredRefusalChangesNothing() {
        Customer son = customers.given(registeredWithGoogle(SON_GOOGLE_SUBJECT, "Aisyah's son"));
        PersonVerificationRefusedIE event = customerRefused("refusal-1", SON_PERSON_ID, son.id().value(),
                "UNDER_MINIMUM_AGE");
        handler.handle(event);
        son.recordVerificationRefused(VerificationRefusal.NOT_LOCAL);

        handler.handle(event);

        assertThat(customers.saves()).containsExactly(son.id());
        assertThat(son.verificationRefusal()).contains(VerificationRefusal.NOT_LOCAL);
        assertThat(refusals.recorded()).isEmpty();
        assertThat(HandlePersonVerificationRefusedIE.HANDLER).isEqualTo("customer.PersonVerificationRefused");
    }
}
