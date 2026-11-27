package com.homa.verification.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.homa.verification.domain.exception.IdentityNumberAlreadyHeldException;
import com.homa.verification.domain.exception.PersonUnderageException;
import com.homa.verification.domain.exception.VerificationAccountMismatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class RefusalReasonsTest {

    @Test
    @Tag("CUS-10")
    @Tag("IDN-3")
    @DisplayName("Each refusal is published with the contract's reason, not the rule's message.")
    void eachRefusalHasItsReason() {
        assertThat(RefusalReasons.of(new PersonUnderageException("HLP-1", "a member", 18)))
                .isEqualTo("UNDER_MINIMUM_AGE");
        assertThat(RefusalReasons.of(new IdentityNumberAlreadyHeldException())).isEqualTo("IDENTITY_ALREADY_HELD");
    }

    @Test
    @DisplayName("A refusal the contract has no reason for is the check not passing.")
    void anyOtherRefusalIsTheCheckNotPassing() {
        assertThat(RefusalReasons.of(new VerificationAccountMismatchException("a customer verifies for their account")))
                .isEqualTo("CHECK_NOT_PASSED");
    }
}
