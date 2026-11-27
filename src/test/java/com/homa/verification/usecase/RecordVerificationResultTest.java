package com.homa.verification.usecase;

import static com.homa.fixtures.HomaFixtures.at;
import static com.homa.verification.usecase.VerificationWorld.ADULT_BORN;
import static com.homa.verification.usecase.VerificationWorld.AISYAH_CHECKED_AT;
import static com.homa.verification.usecase.VerificationWorld.AISYAH_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.AZMAN_CHECKED_AT;
import static com.homa.verification.usecase.VerificationWorld.AZMAN_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.CUSTOMER;
import static com.homa.verification.usecase.VerificationWorld.DANIEL_CUSTOMER_ID;
import static com.homa.verification.usecase.VerificationWorld.DANIEL_SECOND_CUSTOMER_ID;
import static com.homa.verification.usecase.VerificationWorld.DANIEL_SECOND_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.DANIEL_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.MEMBER;
import static com.homa.verification.usecase.VerificationWorld.OWNER;
import static com.homa.verification.usecase.VerificationWorld.RINA_CHECKED_AT;
import static com.homa.verification.usecase.VerificationWorld.RINA_IKAD;
import static com.homa.verification.usecase.VerificationWorld.RINA_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.SITI_CHECKED_AT;
import static com.homa.verification.usecase.VerificationWorld.SITI_MYKAD;
import static com.homa.verification.usecase.VerificationWorld.SITI_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.SON_CUSTOMER_ID;
import static com.homa.verification.usecase.VerificationWorld.SON_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.WEBHOOK_DELAY;
import static com.homa.verification.usecase.VerificationWorld.aisyahsAnswer;
import static com.homa.verification.usecase.VerificationWorld.azmansAnswer;
import static com.homa.verification.usecase.VerificationWorld.danielsAnswer;
import static com.homa.verification.usecase.VerificationWorld.localMyKad;
import static com.homa.verification.usecase.VerificationWorld.rinasAnswer;
import static com.homa.verification.usecase.VerificationWorld.sitisAnswer;
import static com.homa.verification.usecase.VerificationWorld.sonsAnswer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.ie.events.PersonVerificationRefusedIE;
import com.homa.ie.events.PersonVerifiedIE;
import com.homa.ie.port.VerificationIE;
import com.homa.verification.domain.PersonId;
import com.homa.verification.domain.PersonStatus;
import com.homa.verification.domain.VerificationAttempt;
import com.homa.verification.domain.VerificationAttemptState;
import com.homa.verification.domain.VerifiedPerson;
import com.homa.verification.usecase.dto.RecordVerificationResultRequest;
import com.homa.verification.usecase.dto.RecordVerificationResultResponse;
import com.homa.verification.usecase.dto.StartVerificationResponse;
import com.homa.verification.usecase.exception.VerificationNotFoundException;
import com.homa.verification.usecase.port.out.VerificationAnswer;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RecordVerificationResultTest {

    private final VerificationWorld world = new VerificationWorld();

    private RecordVerificationResultResponse verified(StartVerificationResponse started, String role) {
        return new RecordVerificationResultResponse("VERIFIED", started.verificationId(), started.personId(), role,
                null, null);
    }

    private RecordVerificationResultResponse refused(StartVerificationResponse started, String role, String rule,
            String reason) {
        return new RecordVerificationResultResponse("REFUSED", started.verificationId(), started.personId(), role,
                rule, reason);
    }

    /** The refusal was recorded on the verification, and nobody was verified. */
    private void assertRecordedRefusal(StartVerificationResponse started, String rule) {
        VerificationAttempt attempt = world.attempts.findById(started.verificationId()).orElseThrow();
        assertThat(world.attempts.savedStateOf(started.verificationId())).isEqualTo(VerificationAttemptState.REFUSED);
        assertThat(attempt.refusedUnder()).contains(rule);
        assertThat(attempt.decidedAt()).contains(world.time.now());
        assertThat(world.persons.findById(new PersonId(started.personId()))).isEmpty();
        assertThat(world.publisher.published(PersonVerifiedIE.class)).isEmpty();
    }

    /** The refusal as Verification publishes it, decided now. */
    private PersonVerificationRefusedIE refusedEvent(String eventId, StartVerificationResponse started, String role,
            String accountId, String reason, String rule) {
        return new PersonVerificationRefusedIE(eventId, started.personId(), role, accountId, reason, rule,
                world.time.now());
    }

    @Test
    @Tag("CUS-3")
    @Tag("CUS-10")
    @Tag("IDN-1")
    @DisplayName("Aisyah verifies with her MyKad: she's verified as a local adult for her customer account.")
    void aCustomerIsVerifiedAndPersonVerifiedIsPublished() {
        StartVerificationResponse started = world.start(AISYAH_SIGN_IN, CUSTOMER, HomaFixtures.AISYAH_CUSTOMER_ID,
                "MyKad");

        RecordVerificationResultResponse response = world.answer(started, aisyahsAnswer());

        Instant recordedAt = AISYAH_CHECKED_AT.plus(WEBHOOK_DELAY);
        assertThat(response).isEqualTo(verified(started, CUSTOMER));
        assertThat(world.publisher.single(PersonVerifiedIE.class)).isEqualTo(new PersonVerifiedIE(
                "event-1", started.personId(), "CUSTOMER", HomaFixtures.AISYAH_CUSTOMER_ID, HomaFixtures.AISYAH_NAME,
                "LOCAL", true, null, null, recordedAt));

        VerifiedPerson aisyah = world.persons.findById(new PersonId(started.personId())).orElseThrow();
        assertThat(aisyah.status()).isEqualTo(PersonStatus.LOCAL);
        assertThat(aisyah.identityDocumentType().name()).isEqualTo("MyKad");
        assertThat(aisyah.result().checkedAt()).isEqualTo(AISYAH_CHECKED_AT);
        VerificationAttempt attempt = world.attempts.findById(started.verificationId()).orElseThrow();
        assertThat(world.attempts.savedStateOf(started.verificationId())).isEqualTo(VerificationAttemptState.VERIFIED);
        assertThat(attempt.decidedAt()).contains(recordedAt);
    }

    @Test
    @Tag("PRV-1")
    @Tag("MON-1")
    @DisplayName("Azman verifies as an owner; Provider learns he's next due six months after his pass.")
    void anOwnerIsVerifiedWithTheirNextDueDate() {
        StartVerificationResponse started = world.start(AZMAN_SIGN_IN, OWNER, null, "MyKad");

        RecordVerificationResultResponse response = world.answer(started, azmansAnswer());

        assertThat(response).isEqualTo(verified(started, OWNER));
        assertThat(world.publisher.single(PersonVerifiedIE.class)).isEqualTo(new PersonVerifiedIE(
                "event-1", started.personId(), "OWNER", null, "Azman", "LOCAL", true, null, at(2027, 4, 1, 9, 0),
                AZMAN_CHECKED_AT.plus(WEBHOOK_DELAY)));
    }

    @Test
    @Tag("HLP-2")
    @Tag("MON-1")
    @DisplayName("Siti, verified on 20 November, verifies as a local member and is next due on 20 May.")
    void aLocalMemberIsVerified() {
        StartVerificationResponse started = world.start(SITI_SIGN_IN, MEMBER, null, "MyKad");

        world.answer(started, sitisAnswer());

        assertThat(world.publisher.single(PersonVerifiedIE.class)).isEqualTo(new PersonVerifiedIE(
                "event-1", started.personId(), "MEMBER", null, "Siti", "LOCAL", true, null, at(2027, 5, 20, 10, 0),
                SITI_CHECKED_AT.plus(WEBHOOK_DELAY)));
    }

    @Test
    @Tag("HLP-3")
    @Tag("IDN-1")
    @DisplayName("Rina verifies with her i-Kad as a foreign member; its date is her work permit's expiry.")
    void aForeignMemberIsVerifiedWithTheirPermitExpiry() {
        StartVerificationResponse started = world.start(RINA_SIGN_IN, MEMBER, null, "i-Kad");

        world.answer(started, rinasAnswer());

        assertThat(world.publisher.single(PersonVerifiedIE.class)).isEqualTo(new PersonVerifiedIE(
                "event-1", started.personId(), "MEMBER", null, "Rina", "FOREIGN", true, "2027-05-21",
                at(2027, 5, 2, 10, 0), RINA_CHECKED_AT.plus(WEBHOOK_DELAY)));
    }

    @Test
    @Tag("CUS-10")
    @DisplayName("Aisyah's 16-year-old son can't book: his verification is refused under CUS-10 and recorded.")
    void anUnderageCustomerIsRefusedAndTheRefusalRecorded() {
        StartVerificationResponse started = world.start(SON_SIGN_IN, CUSTOMER, SON_CUSTOMER_ID, "MyKad");

        RecordVerificationResultResponse response = world.answer(started, sonsAnswer());

        assertThat(response).isEqualTo(refused(started, CUSTOMER, "CUS-10",
                "CUS-10: a customer must be aged 18 or above"));
        assertRecordedRefusal(started, "CUS-10");
        assertThat(world.publisher.published()).<VerificationIE>containsExactly(refusedEvent("event-1", started,
                CUSTOMER, SON_CUSTOMER_ID, "UNDER_MINIMUM_AGE", "CUS-10"));
    }

    @Test
    @Tag("CUS-10")
    @Tag("PRV-1")
    @DisplayName("Rina is foreign, so in Malaysia she can be neither a customer nor an owner.")
    void aStatusTheRoleDoesNotAllowIsRefused() {
        StartVerificationResponse asCustomer = world.start(RINA_SIGN_IN, CUSTOMER, "customer-rina", "i-Kad");
        StartVerificationResponse asOwner = world.start(RINA_SIGN_IN, OWNER, null, "i-Kad");

        assertThat(world.answer(asCustomer, rinasAnswer())).isEqualTo(refused(asCustomer, CUSTOMER, "CUS-10",
                "CUS-10: in Malaysia a customer can't be foreign"));
        assertThat(world.answer(asOwner, rinasAnswer())).isEqualTo(refused(asOwner, OWNER, "PRV-1",
                "PRV-1: in Malaysia an owner can't be foreign"));
        assertRecordedRefusal(asOwner, "PRV-1");
        assertThat(world.publisher.published()).<VerificationIE>containsExactly(
                refusedEvent("event-1", asCustomer, CUSTOMER, "customer-rina", "STATUS_NOT_ALLOWED_FOR_ROLE",
                        "CUS-10"),
                refusedEvent("event-2", asOwner, OWNER, null, "STATUS_NOT_ALLOWED_FOR_ROLE", "PRV-1"));
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("When the service doesn't pass the document, the verification is refused under IDN-1.")
    void anUnpassedCheckIsRefused() {
        StartVerificationResponse started = world.start(AISYAH_SIGN_IN, CUSTOMER, HomaFixtures.AISYAH_CUSTOMER_ID,
                "MyKad");

        RecordVerificationResultResponse response = world.answer(started,
                new VerificationAnswer.NotPassed(AISYAH_CHECKED_AT));

        assertThat(response).isEqualTo(refused(started, CUSTOMER, "IDN-1",
                "IDN-1: the identity document did not pass verification, so the person can't be verified"));
        assertRecordedRefusal(started, "IDN-1");
        assertThat(world.publisher.published()).<VerificationIE>containsExactly(refusedEvent("event-1", started,
                CUSTOMER, HomaFixtures.AISYAH_CUSTOMER_ID, "CHECK_NOT_PASSED", "IDN-1"));
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("A document the service read but the policy doesn't set for the person's status is refused.")
    void aDocumentNotSetForTheStatusIsRefused() {
        StartVerificationResponse passport = world.start(RINA_SIGN_IN, MEMBER, null, "i-Kad");
        StartVerificationResponse localIKad = world.start(SITI_SIGN_IN, MEMBER, null, "MyKad");

        assertThat(world.answer(passport, new VerificationAnswer.Passed("Rina", PersonStatus.FOREIGN, "Passport",
                RINA_IKAD, ADULT_BORN, LocalDate.of(2030, 1, 1), RINA_CHECKED_AT)))
                .isEqualTo(refused(passport, MEMBER, "IDN-1", "IDN-1: Malaysia doesn't verify anyone with Passport"));
        assertThat(world.answer(localIKad, new VerificationAnswer.Passed("Siti", PersonStatus.LOCAL, "i-Kad",
                SITI_MYKAD, ADULT_BORN, null, SITI_CHECKED_AT)))
                .isEqualTo(refused(localIKad, MEMBER, "IDN-1",
                        "IDN-1: in Malaysia a local person isn't verified with i-Kad"));
        assertRecordedRefusal(localIKad, "IDN-1");
        assertThat(world.publisher.published(PersonVerificationRefusedIE.class))
                .extracting(PersonVerificationRefusedIE::reason, PersonVerificationRefusedIE::refusedUnder)
                .containsExactly(tuple("DOCUMENT_NOT_ACCEPTED", "IDN-1"), tuple("DOCUMENT_NOT_ACCEPTED", "IDN-1"));
        assertThat(world.publisher.published().getLast())
                .isEqualTo(refusedEvent("event-2", localIKad, MEMBER, null, "DOCUMENT_NOT_ACCEPTED", "IDN-1"));
    }

    @Test
    @Tag("HLP-3")
    @Tag("IDN-2")
    @DisplayName("An i-Kad without its date, or an answer without a name, can't verify anyone.")
    void anAnswerMissingAFactIsRefused() {
        StartVerificationResponse noExpiry = world.start(RINA_SIGN_IN, MEMBER, null, "i-Kad");
        StartVerificationResponse noName = world.start(SITI_SIGN_IN, MEMBER, null, "MyKad");

        assertThat(world.answer(noExpiry, new VerificationAnswer.Passed("Rina", PersonStatus.FOREIGN, "i-Kad",
                RINA_IKAD, ADULT_BORN, null, RINA_CHECKED_AT)).refusedUnder()).isEqualTo("HLP-3");
        assertThat(world.answer(noName, localMyKad(" ", SITI_MYKAD, ADULT_BORN, SITI_CHECKED_AT)))
                .isEqualTo(refused(noName, MEMBER, "IDN-2", "IDN-2: a verified person must have a name"));
        assertRecordedRefusal(noName, "IDN-2");
        assertThat(world.publisher.published(PersonVerificationRefusedIE.class))
                .extracting(PersonVerificationRefusedIE::reason, PersonVerificationRefusedIE::refusedUnder)
                .containsExactly(tuple("DOCUMENT_NOT_ACCEPTED", "HLP-3"), tuple("DOCUMENT_NOT_ACCEPTED", "IDN-2"));
    }

    @Test
    @Tag("IDN-3")
    @Tag("CUS-10")
    @DisplayName("Daniel can't open a second account with another Google account: his MyKad is already used.")
    void aNumberHeldByAnotherPersonIsRefused() {
        world.verify(DANIEL_SIGN_IN, CUSTOMER, DANIEL_CUSTOMER_ID, danielsAnswer(at(2026, 11, 24, 18, 0)));
        world.publisher.clear();
        StartVerificationResponse second = world.start(DANIEL_SECOND_SIGN_IN, CUSTOMER, DANIEL_SECOND_CUSTOMER_ID,
                "MyKad");

        RecordVerificationResultResponse response = world.answer(second, danielsAnswer(at(2026, 11, 25, 9, 0)));

        assertThat(response).isEqualTo(refused(second, CUSTOMER, "IDN-3",
                "IDN-3: this identity document is already verified for another person"));
        assertRecordedRefusal(second, "IDN-3");
        assertThat(world.publisher.published()).<VerificationIE>containsExactly(refusedEvent("event-2", second,
                CUSTOMER, DANIEL_SECOND_CUSTOMER_ID, "IDENTITY_ALREADY_HELD", "IDN-3"));
        assertThat(world.persons.size()).isEqualTo(1);
    }

    @Test
    @Tag("IDN-3")
    @Tag("CUS-3")
    @DisplayName("Siti, a verified member, verifies again as a customer and keeps her personId.")
    void aVerifiedPersonVerifiesAgainForAnotherRole() {
        RecordVerificationResultResponse asMember = world.verify(SITI_SIGN_IN, MEMBER, null, sitisAnswer());
        world.publisher.clear();
        Instant later = SITI_CHECKED_AT.plus(Duration.ofDays(3));

        RecordVerificationResultResponse asCustomer = world.verify(SITI_SIGN_IN, CUSTOMER, "customer-siti",
                localMyKad("Siti", SITI_MYKAD, ADULT_BORN, later));

        assertThat(asCustomer.outcome()).isEqualTo("VERIFIED");
        assertThat(asCustomer.personId()).isEqualTo(asMember.personId());
        assertThat(world.publisher.single(PersonVerifiedIE.class)).isEqualTo(new PersonVerifiedIE(
                "event-2", asMember.personId(), "CUSTOMER", "customer-siti", "Siti", "LOCAL", true, null, null,
                later.plus(WEBHOOK_DELAY)));
        assertThat(world.persons.size()).isEqualTo(1);
        assertThat(world.persons.findById(new PersonId(asMember.personId())).orElseThrow().result().checkedAt())
                .isEqualTo(later);
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("A repeated webhook is a no-op: Aisyah is verified once and PersonVerifiedIE is published once.")
    void aDuplicateWebhookChangesNothing() {
        StartVerificationResponse started = world.start(AISYAH_SIGN_IN, CUSTOMER, HomaFixtures.AISYAH_CUSTOMER_ID,
                "MyKad");
        world.verificationService.answers(world.referenceOf(started), aisyahsAnswer());
        world.webhook("notification-7", started);
        int saves = world.attempts.saveCount();

        RecordVerificationResultResponse repeat = world.webhook("notification-7", started);

        assertThat(repeat).isEqualTo(new RecordVerificationResultResponse("DUPLICATE", null, null, null, null, null));
        assertThat(world.publisher.published()).hasSize(1);
        assertThat(world.verificationService.resultsAsked()).hasSize(1);
        assertThat(world.attempts.saveCount()).isEqualTo(saves);
        assertThat(world.processedMessages.handled("notification-7", RecordVerificationResult.HANDLER)).isTrue();
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("A later notification for a decided verification returns the recorded outcome and decides nothing.")
    void aDecidedVerificationIsNotDecidedAgain() {
        StartVerificationResponse started = world.start(SON_SIGN_IN, CUSTOMER, SON_CUSTOMER_ID, "MyKad");
        world.answer(started, sonsAnswer());
        world.verificationService.answers(world.referenceOf(started), aisyahsAnswer());
        int saves = world.attempts.saveCount();
        List<VerificationIE> publishedOnce = world.publisher.published();

        RecordVerificationResultResponse again = world.webhook(world.nextNotificationId(), started);

        assertThat(again).isEqualTo(refused(started, CUSTOMER, "CUS-10", "CUS-10: a customer must be aged 18 or above"));
        assertThat(world.verificationService.resultsAsked()).hasSize(1);
        assertThat(world.attempts.saveCount()).isEqualTo(saves);
        assertThat(publishedOnce).hasSize(1);
        assertThat(world.publisher.published()).isEqualTo(publishedOnce);
    }

    @Test
    @DisplayName("A webhook before the service has an answer leaves the verification pending.")
    void aWebhookWithoutAnAnswerLeavesItPending() {
        StartVerificationResponse started = world.start(AZMAN_SIGN_IN, OWNER, null, "MyKad");
        int saves = world.attempts.saveCount();

        RecordVerificationResultResponse early = world.webhook(world.nextNotificationId(), started);

        assertThat(early).isEqualTo(new RecordVerificationResultResponse("PENDING", started.verificationId(),
                started.personId(), OWNER, null, null));
        assertThat(world.attempts.saveCount()).isEqualTo(saves);
        assertThat(world.publisher.published()).isEmpty();
        assertThat(world.answer(started, azmansAnswer()).outcome()).isEqualTo("VERIFIED");
    }

    @Test
    @DisplayName("A webhook for a check Homa never started is refused.")
    void aWebhookForAnUnknownCheckIsRefused() {
        assertThatThrownBy(() -> world.recorder.record(new RecordVerificationResultRequest("notification-1",
                "service-ref-unknown")))
                .isInstanceOf(VerificationNotFoundException.class)
                .hasMessage("No verification is known by the service reference service-ref-unknown");
        assertThat(world.publisher.published()).isEmpty();
    }
}
