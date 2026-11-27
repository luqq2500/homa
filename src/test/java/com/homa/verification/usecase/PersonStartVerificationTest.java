package com.homa.verification.usecase;

import static com.homa.verification.usecase.VerificationWorld.AISYAH_MYKAD;
import static com.homa.verification.usecase.VerificationWorld.AISYAH_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.AZMAN_SIGN_IN;
import static com.homa.verification.usecase.VerificationWorld.CUSTOMER;
import static com.homa.verification.usecase.VerificationWorld.MEMBER;
import static com.homa.verification.usecase.VerificationWorld.OWNER;
import static com.homa.verification.usecase.VerificationWorld.RINA_SIGN_IN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.fixtures.HomaFixtures;
import com.homa.verification.domain.PersonId;
import com.homa.verification.domain.PersonRole;
import com.homa.verification.domain.VerificationAttempt;
import com.homa.verification.domain.VerificationAttemptState;
import com.homa.verification.domain.exception.DocumentNotAcceptedException;
import com.homa.verification.domain.exception.VerificationAccountMismatchException;
import com.homa.verification.usecase.dto.StartVerificationRequest;
import com.homa.verification.usecase.dto.StartVerificationResponse;
import com.homa.verification.usecase.exception.UnknownRoleException;
import com.homa.verification.usecase.port.out.VerificationSubmission;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class PersonStartVerificationTest {

    private final VerificationWorld world = new VerificationWorld();

    @Test
    @Tag("CUS-3")
    @Tag("IDN-1")
    @DisplayName("Aisyah taps Reserve and starts verifying with her MyKad, for her customer account, before paying.")
    void aCustomerStartsVerifyingForTheirAccount() {
        StartVerificationResponse started = world.starter.start(new StartVerificationRequest(AISYAH_SIGN_IN, CUSTOMER,
                HomaFixtures.AISYAH_CUSTOMER_ID, "MyKad", AISYAH_MYKAD));

        VerificationSubmission submission = world.verificationService.submissions().get(0);
        assertThat(world.verificationService.submissions()).hasSize(1);
        assertThat(submission.verificationId()).isEqualTo(started.verificationId());
        assertThat(submission.documentType()).isEqualTo("MyKad");
        assertThat(submission.documentNumber()).isEqualTo(AISYAH_MYKAD);

        String reference = world.referenceOf(started);
        assertThat(started.handoff()).isEqualTo("https://verify.example/session/" + reference);
        assertThat(started.state()).isEqualTo("PENDING");
        assertThat(started.personId()).isNotBlank();

        VerificationAttempt saved = world.attempts.findById(started.verificationId()).orElseThrow();
        assertThat(world.attempts.size()).isEqualTo(1);
        assertThat(saved.personId().value()).isEqualTo(started.personId());
        assertThat(saved.role()).isEqualTo(PersonRole.CUSTOMER);
        assertThat(saved.accountId()).contains(HomaFixtures.AISYAH_CUSTOMER_ID);
        assertThat(saved.serviceReference()).contains(reference);
        assertThat(saved.startedAt()).isEqualTo(world.time.now());
        assertThat(world.attempts.savedStateOf(started.verificationId())).isEqualTo(VerificationAttemptState.PENDING);
        assertThat(world.publisher.published()).isEmpty();
    }

    @Test
    @Tag("IDN-3")
    @DisplayName("Aisyah is given a personId at her first verification and keeps it for every later one.")
    void theSignedInUserKeepsTheirPersonId() {
        StartVerificationResponse first = world.start(AISYAH_SIGN_IN, CUSTOMER, HomaFixtures.AISYAH_CUSTOMER_ID, "MyKad");
        StartVerificationResponse second = world.start(AISYAH_SIGN_IN, MEMBER, null, "MyKad");
        StartVerificationResponse azman = world.start(AZMAN_SIGN_IN, OWNER, null, "MyKad");

        assertThat(world.signIns.personIdOf(AISYAH_SIGN_IN)).contains(new PersonId(first.personId()));
        assertThat(second.personId()).isEqualTo(first.personId());
        assertThat(second.verificationId()).isNotEqualTo(first.verificationId());
        assertThat(azman.personId()).isNotEqualTo(first.personId());
        assertThat(world.signIns.personIdOf(AZMAN_SIGN_IN)).contains(new PersonId(azman.personId()));
    }

    @Test
    @Tag("PRV-1")
    @Tag("HLP-3")
    @DisplayName("Azman starts as an owner and Rina as a member with her i-Kad, neither for a customer account.")
    void anOwnerAndAMemberStartWithoutAnAccount() {
        StartVerificationResponse azman = world.start(AZMAN_SIGN_IN, OWNER, null, "MyKad");
        StartVerificationResponse rina = world.start(RINA_SIGN_IN, MEMBER, null, "i-Kad");

        VerificationAttempt azmans = world.attempts.findById(azman.verificationId()).orElseThrow();
        assertThat(azmans.role()).isEqualTo(PersonRole.OWNER);
        assertThat(azmans.accountId()).isEmpty();
        assertThat(world.attempts.findById(rina.verificationId()).orElseThrow().role()).isEqualTo(PersonRole.MEMBER);
        assertThat(world.verificationService.submissions().get(1).documentType()).isEqualTo("i-Kad");
        assertThat(world.verificationService.submissions().get(1).documentNumber()).isNull();
    }

    @Test
    @Tag("IDN-1")
    @DisplayName("A foreign candidate who offers a passport is refused before anything reaches the service.")
    void aDocumentThePolicyDoesNotNameIsRefusedAtOnce() {
        assertThatThrownBy(() -> world.start(RINA_SIGN_IN, MEMBER, null, "Passport"))
                .isInstanceOf(DocumentNotAcceptedException.class)
                .hasMessage("IDN-1: Malaysia doesn't verify anyone with Passport");

        assertThat(world.verificationService.submissions()).isEmpty();
        assertThat(world.attempts.size()).isZero();
    }

    @Test
    @Tag("CUS-3")
    @DisplayName("A customer's verification needs the account that asked; an owner's names none.")
    void theAccountMustFitTheRole() {
        assertThatThrownBy(() -> world.start(AISYAH_SIGN_IN, CUSTOMER, null, "MyKad"))
                .isInstanceOf(VerificationAccountMismatchException.class);
        assertThatThrownBy(() -> world.start(AZMAN_SIGN_IN, OWNER, "customer-azman", "MyKad"))
                .isInstanceOf(VerificationAccountMismatchException.class);

        assertThat(world.verificationService.submissions()).isEmpty();
        assertThat(world.attempts.size()).isZero();
    }

    @Test
    @DisplayName("A person verifies as a customer, an owner or a member, and nothing else.")
    void anUnknownRoleIsRefused() {
        assertThatThrownBy(() -> world.start(AISYAH_SIGN_IN, "SUPPORT", null, "MyKad"))
                .isInstanceOf(UnknownRoleException.class)
                .hasMessage("A person verifies as CUSTOMER, OWNER or MEMBER, not SUPPORT");
        assertThatThrownBy(() -> world.start(AISYAH_SIGN_IN, null, null, "MyKad"))
                .isInstanceOf(UnknownRoleException.class);

        assertThat(world.signIns.personIdOf(AISYAH_SIGN_IN)).isEmpty();
        assertThat(world.verificationService.submissions()).isEmpty();
    }
}
