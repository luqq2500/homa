package com.homa.account.domain;

import static com.homa.account.domain.AccountBuilders.CODE;
import static com.homa.account.domain.AccountBuilders.PHONE;
import static com.homa.account.domain.AccountBuilders.aPhoneVerificationStartedAt;
import static com.homa.account.domain.AccountBuilders.at;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.account.domain.exception.InvalidVerificationCodeException;
import com.homa.account.domain.exception.PhoneAlreadyConfirmedException;
import com.homa.account.domain.exception.TooManyVerificationAttemptsException;
import com.homa.account.domain.exception.VerificationCodeExpiredException;
import java.time.Duration;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Feature: Customer registration, confirming the phone by SMS code. */
class PhoneVerificationConfirmationTest {

    @Nested
    class StartingAVerification {

        @Test
        void aNewVerificationIsNotYetConfirmed() {
            PhoneVerification verification = aPhoneVerificationStartedAt("10:00");

            assertThat(verification.isConfirmed()).isFalse();
            assertThat(verification.phone()).isEqualTo(PHONE);
            assertThat(verification.wrongAttempts()).isZero();
            assertThat(verification.id()).isNotBlank();
        }

        @Test
        void theCodeIsValidForFiveMinutes() {
            assertThat(aPhoneVerificationStartedAt("10:00").expiresAt()).isEqualTo(at("10:05"));
            assertThat(PhoneVerification.CODE_VALIDITY).isEqualTo(Duration.ofMinutes(5));
        }

        @ParameterizedTest(name = "\"{0}\" is not a code")
        @NullSource
        @ValueSource(strings = {"48291", "4829134", "48291a", ""})
        void theCodeIsSixDigits(String code) {
            assertThatThrownBy(() -> PhoneVerification.start(PHONE, code, at("10:00")))
                    .isInstanceOf(InvalidVerificationCodeException.class);
        }

        @Test
        void aVerificationNeedsAPhoneNumber() {
            assertThatThrownBy(() -> PhoneVerification.start(null, CODE, at("10:00")))
                    .isInstanceOf(NullPointerException.class);
        }
    }

    @Nested
    class EnteringTheCode {

        @Test
        void theRightCodeConfirmsThePhone() {
            PhoneVerification verification = aPhoneVerificationStartedAt("10:00");

            assertThat(verification.confirm(CODE, at("10:02"))).isTrue();
            assertThat(verification.isConfirmed()).isTrue();
            assertThat(verification.wrongAttempts()).isZero();
        }

        @Test
        void aWrongCodeDoesNotConfirmThePhone() {
            PhoneVerification verification = aPhoneVerificationStartedAt("10:00");

            assertThat(verification.confirm("000000", at("10:02"))).isFalse();
            assertThat(verification.isConfirmed()).isFalse();
            assertThat(verification.wrongAttempts()).isEqualTo(1);
        }

        @Test
        void theRightCodeAfterAWrongOneStillConfirms() {
            PhoneVerification verification = aPhoneVerificationStartedAt("10:00");
            verification.confirm("000000", at("10:01"));

            assertThat(verification.confirm(CODE, at("10:02"))).isTrue();
            assertThat(verification.wrongAttempts()).isEqualTo(1);
        }

        @Test
        void anExpiredCodeIsRejected() {
            PhoneVerification verification = aPhoneVerificationStartedAt("10:00");

            assertThatThrownBy(() -> verification.confirm(CODE, at("10:06")))
                    .isInstanceOf(VerificationCodeExpiredException.class);
            assertThat(verification.isConfirmed()).isFalse();
        }

        @Test
        void theCodeExpiresExactlyFiveMinutesAfterItWasSent() {
            assertThat(aPhoneVerificationStartedAt("10:00").confirm(CODE, at("10:04:59"))).isTrue();
            assertThatThrownBy(() -> aPhoneVerificationStartedAt("10:00").confirm(CODE, at("10:05")))
                    .isInstanceOf(VerificationCodeExpiredException.class);
        }

        @Test
        void aConfirmedPhoneIsNotConfirmedAgain() {
            PhoneVerification verification = aPhoneVerificationStartedAt("10:00");
            verification.confirm(CODE, at("10:01"));

            assertThatThrownBy(() -> verification.confirm(CODE, at("10:02")))
                    .isInstanceOf(PhoneAlreadyConfirmedException.class);
        }
    }

    @Nested
    class TooManyWrongCodes {

        @Test
        void fiveWrongCodesMeanANewCodeIsNeeded() {
            PhoneVerification verification = aPhoneVerificationStartedAt("10:00");
            for (int attempt = 0; attempt < 5; attempt++) {
                assertThat(verification.confirm("000000", at("10:01"))).isFalse();
            }

            assertThatThrownBy(() -> verification.confirm(CODE, at("10:02")))
                    .isInstanceOf(TooManyVerificationAttemptsException.class)
                    .hasMessageContaining("5 times");
            assertThat(verification.isConfirmed()).isFalse();
        }

        @Test
        void theRightCodeOnTheFifthAttemptStillConfirms() {
            PhoneVerification verification = aPhoneVerificationStartedAt("10:00");
            for (int attempt = 0; attempt < 4; attempt++) {
                verification.confirm("000000", at("10:01"));
            }

            assertThat(verification.confirm(CODE, at("10:02"))).isTrue();
        }
    }
}
