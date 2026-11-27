package com.homa.verification.domain;

import static com.homa.verification.domain.VerificationFixtures.AISYAH;
import static com.homa.verification.domain.VerificationFixtures.AISYAH_CHECKED_AT;
import static com.homa.verification.domain.VerificationFixtures.AZMAN;
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

class VerificationAttemptTest {

    private static final VerificationAttemptId ID = new VerificationAttemptId("attempt-1");
    private static final String REFERENCE = "service-ref-1";
    private static final Instant STARTED_AT = AISYAH_CHECKED_AT.minus(Duration.ofMinutes(2));
    private static final Instant ANSWERED_AT = AISYAH_CHECKED_AT;

    private static VerificationAttempt aisyahsAttempt() {
        return VerificationAttempt.start(ID, AISYAH, PersonRole.CUSTOMER, HomaFixtures.AISYAH_CUSTOMER_ID, REFERENCE,
                STARTED_AT);
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("Aisyah verifies at her first reservation for her customer account, and waits for the service.")
    void aCustomerVerifiesForTheirAccountAndWaitsForTheService() {
        VerificationAttempt attempt = aisyahsAttempt();

        assertThat(attempt.id()).isEqualTo(ID);
        assertThat(attempt.personId()).isEqualTo(AISYAH);
        assertThat(attempt.role()).isEqualTo(PersonRole.CUSTOMER);
        assertThat(attempt.accountId()).contains(HomaFixtures.AISYAH_CUSTOMER_ID);
        assertThat(attempt.serviceReference()).isEqualTo(REFERENCE);
        assertThat(attempt.startedAt()).isEqualTo(STARTED_AT);
        assertThat(attempt.state()).isEqualTo(VerificationAttemptState.PENDING);
        assertThat(attempt.isPending()).isTrue();
        assertThat(attempt.refusedUnder()).isEmpty();
        assertThat(attempt.refusalReason()).isEmpty();
        assertThat(attempt.decidedAt()).isEmpty();
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("A customer's verification without the account that asked for it can't start.")
    void aCustomersVerificationNeedsTheirAccount() {
        assertThatThrownBy(() -> VerificationAttempt.start(ID, AISYAH, PersonRole.CUSTOMER, null, REFERENCE, STARTED_AT))
                .isInstanceOf(VerificationAccountMismatchException.class)
                .hasMessage("CUS-3: a customer verifies for their customer account");
        assertThatThrownBy(() -> VerificationAttempt.start(ID, AISYAH, PersonRole.CUSTOMER, " ", REFERENCE, STARTED_AT))
                .isInstanceOf(VerificationAccountMismatchException.class);
    }

    @Test
    @Tag("PRV-1")
    @Tag("HLP-1")
    @DisplayName("Azman, as an owner, and Siti, as a member, verify without a customer account.")
    void anOwnerOrAMemberVerifiesWithoutACustomerAccount() {
        VerificationAttempt azman = VerificationAttempt.start(ID, AZMAN, PersonRole.OWNER, null, REFERENCE, STARTED_AT);
        VerificationAttempt siti = VerificationAttempt.start(ID, SITI, PersonRole.MEMBER, "", REFERENCE, STARTED_AT);

        assertThat(azman.accountId()).isEmpty();
        assertThat(siti.accountId()).isEmpty();
        assertThat(siti.role()).isEqualTo(PersonRole.MEMBER);
        assertThatThrownBy(() -> VerificationAttempt.start(ID, AZMAN, PersonRole.OWNER, "customer-azman", REFERENCE,
                STARTED_AT))
                .isInstanceOf(VerificationAccountMismatchException.class)
                .hasMessage("CUS-3: an owner verifies without a customer account");
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("When the service passes Aisyah's MyKad, her verification is recorded as verified.")
    void aPassIsRecorded() {
        VerificationAttempt attempt = aisyahsAttempt();

        attempt.recordPass(ANSWERED_AT);

        assertThat(attempt.state()).isEqualTo(VerificationAttemptState.VERIFIED);
        assertThat(attempt.isPending()).isFalse();
        assertThat(attempt.decidedAt()).contains(ANSWERED_AT);
        assertThat(attempt.refusedUnder()).isEmpty();
    }

    @Test
    @Tag("IDN-3")
    @DisplayName("Daniel's second account is refused under IDN-3, and the refusal is kept with its rule.")
    void aRefusalIsRecordedWithItsRule() {
        VerificationAttempt attempt = aisyahsAttempt();

        attempt.recordRefusal(new IdentityNumberAlreadyHeldException(), ANSWERED_AT);

        assertThat(attempt.state()).isEqualTo(VerificationAttemptState.REFUSED);
        assertThat(attempt.isPending()).isFalse();
        assertThat(attempt.refusedUnder()).contains("IDN-3");
        assertThat(attempt.refusalReason()).contains("IDN-3: this identity document is already verified for another person");
        assertThat(attempt.decidedAt()).contains(ANSWERED_AT);
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("When the service doesn't pass the document, the verification is refused under IDN-1.")
    void anUnpassedCheckIsRefusedUnderIdn1() {
        VerificationAttempt attempt = aisyahsAttempt();

        attempt.recordUnpassedCheck(ANSWERED_AT);

        assertThat(attempt.state()).isEqualTo(VerificationAttemptState.REFUSED);
        assertThat(attempt.refusedUnder()).contains("IDN-1");
        assertThat(attempt.decidedAt()).contains(ANSWERED_AT);
    }

    @Test
    @Tag("IDN-2")
    @DisplayName("A verification is decided once: a second answer can't change a recorded result.")
    void aVerificationIsDecidedOnce() {
        VerificationAttempt verified = aisyahsAttempt();
        verified.recordPass(ANSWERED_AT);
        VerificationAttempt refused = aisyahsAttempt();
        refused.recordUnpassedCheck(ANSWERED_AT);

        assertThatThrownBy(() -> verified.recordUnpassedCheck(ANSWERED_AT.plusSeconds(1)))
                .isInstanceOf(VerificationAlreadyDecidedException.class)
                .hasMessage("IDN-2: this verification is already verified, so it can't be decided again");
        assertThatThrownBy(() -> refused.recordPass(ANSWERED_AT.plusSeconds(1)))
                .isInstanceOf(VerificationAlreadyDecidedException.class)
                .hasMessage("IDN-2: this verification is already refused, so it can't be decided again");
        assertThat(verified.state()).isEqualTo(VerificationAttemptState.VERIFIED);
        assertThat(verified.decidedAt()).contains(ANSWERED_AT);
        assertThat(refused.state()).isEqualTo(VerificationAttemptState.REFUSED);
        assertThat(refused.refusedUnder()).contains("IDN-1");
    }

    @Test
    @DisplayName("A verification attempt id is unique and never blank.")
    void attemptIdsAreUniqueAndNeverBlank() {
        assertThat(VerificationAttemptId.generate()).isNotEqualTo(VerificationAttemptId.generate());
        assertThat(VerificationAttemptId.generate().value()).isNotBlank();
        assertThatThrownBy(() -> new VerificationAttemptId(null)).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("A verification attempt id must not be blank");
        assertThatThrownBy(() -> new VerificationAttemptId(" ")).isInstanceOf(IllegalArgumentException.class);
    }
}
