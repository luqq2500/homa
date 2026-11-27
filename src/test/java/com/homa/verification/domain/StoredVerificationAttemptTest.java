package com.homa.verification.domain;

import static com.homa.verification.domain.VerificationFixtures.AISYAH;
import static com.homa.verification.domain.VerificationFixtures.AISYAH_CHECKED_AT;
import static com.homa.verification.domain.VerificationFixtures.SITI;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.verification.domain.exception.IdentityNumberAlreadyHeldException;
import com.homa.verification.domain.exception.VerificationAccountMismatchException;
import com.homa.verification.domain.exception.VerificationAlreadyDecidedException;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class StoredVerificationAttemptTest {

    private static final VerificationAttemptId ID = new VerificationAttemptId("attempt-1");
    private static final String REFERENCE = "service-ref-1";
    private static final Instant STARTED_AT = AISYAH_CHECKED_AT.minus(Duration.ofMinutes(2));
    private static final Instant ANSWERED_AT = AISYAH_CHECKED_AT;

    private static VerificationAttempt aisyahsAttempt() {
        VerificationAttempt attempt = VerificationAttempt.start(ID, AISYAH, PersonRole.CUSTOMER,
                HomaFixtures.AISYAH_CUSTOMER_ID, STARTED_AT);
        attempt.submittedAs(REFERENCE);
        return attempt;
    }

    private static VerificationAttempt restored(VerificationAttempt stored) {
        return stored(stored, stored.state(), stored.refusedUnder().orElse(null),
                stored.refusalReason().orElse(null), stored.decidedAt().orElse(null));
    }

    private static VerificationAttempt stored(VerificationAttempt from, VerificationAttemptState state,
            String refusedUnder, String refusalReason, Instant decidedAt) {
        return VerificationAttempt.restore(from.id(), from.personId(), from.role(), from.accountId().orElse(null),
                from.startedAt(), from.serviceReference().orElse(null), state, refusedUnder, refusalReason,
                decidedAt);
    }

    private static void assertRefused(VerificationAttemptState state, String refusedUnder, String refusalReason,
            Instant decidedAt) {
        assertThatThrownBy(() -> stored(aisyahsAttempt(), state, refusedUnder, refusalReason, decidedAt))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("A stored verification attempt needs ");
    }

    @Test
    @Tag("EXT-1")
    @DisplayName("Persistence: Aisyah's pending attempt comes back with the service's reference, and her pass is still recorded once it arrives.")
    void aStoredPendingAttemptCanStillBeDecided() {
        VerificationAttempt pending = aisyahsAttempt();
        VerificationAttempt unsubmitted = VerificationAttempt.start(ID, SITI, PersonRole.MEMBER, null, STARTED_AT);

        VerificationAttempt stored = restored(pending);

        assertThat(stored).usingRecursiveComparison().isEqualTo(pending);
        assertThat(restored(unsubmitted)).usingRecursiveComparison().isEqualTo(unsubmitted);
        stored.recordPass(ANSWERED_AT);
        assertThat(stored.state()).isEqualTo(VerificationAttemptState.VERIFIED);
        assertThat(stored.decidedAt()).contains(ANSWERED_AT);
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("Persistence: a verified or refused attempt comes back with its decision as recorded, and is never decided again.")
    void aStoredDecidedAttemptKeepsItsDecision() {
        VerificationAttempt verified = aisyahsAttempt();
        verified.recordPass(ANSWERED_AT);
        VerificationAttempt refused = aisyahsAttempt();
        refused.recordRefusal(new IdentityNumberAlreadyHeldException(), ANSWERED_AT);

        VerificationAttempt storedVerified = restored(verified);
        VerificationAttempt storedRefused = restored(refused);

        assertThat(storedVerified).usingRecursiveComparison().isEqualTo(verified);
        assertThat(storedRefused).usingRecursiveComparison().isEqualTo(refused);
        assertThat(storedRefused.refusedUnder()).contains("IDN-3");
        assertThatThrownBy(() -> storedVerified.recordPass(ANSWERED_AT))
                .isInstanceOf(VerificationAlreadyDecidedException.class);
        assertThatThrownBy(() -> storedRefused.submittedAs(REFERENCE))
                .isInstanceOf(VerificationAlreadyDecidedException.class);
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("Persistence: a stored attempt has a decision time exactly once decided, and a rule and reason exactly when refused.")
    void aStoredDecisionThatDoesNotFitTogetherIsRefused() {
        assertRefused(VerificationAttemptState.PENDING, null, null, ANSWERED_AT);
        assertRefused(VerificationAttemptState.VERIFIED, null, null, null);
        assertRefused(VerificationAttemptState.REFUSED, null, null, ANSWERED_AT);
        assertRefused(VerificationAttemptState.VERIFIED, "IDN-3", "IDN-3: held", ANSWERED_AT);
        assertRefused(VerificationAttemptState.REFUSED, "IDN-3", null, ANSWERED_AT);
        assertRefused(VerificationAttemptState.VERIFIED, null, "IDN-3: held", ANSWERED_AT);
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("Persistence: a stored customer's attempt names their account and another role's names none, and every attempt has a state.")
    void aStoredAttemptsAccountMustFitItsRole() {
        assertThatThrownBy(() -> VerificationAttempt.restore(ID, AISYAH, PersonRole.CUSTOMER, null, STARTED_AT,
                REFERENCE, VerificationAttemptState.PENDING, null, null, null))
                .isInstanceOf(VerificationAccountMismatchException.class);
        assertThatThrownBy(() -> VerificationAttempt.restore(ID, SITI, PersonRole.MEMBER, "customer-siti",
                STARTED_AT, REFERENCE, VerificationAttemptState.PENDING, null, null, null))
                .isInstanceOf(VerificationAccountMismatchException.class);
        assertThatThrownBy(() -> stored(aisyahsAttempt(), null, null, null, null))
                .isInstanceOf(NullPointerException.class);
    }
}
