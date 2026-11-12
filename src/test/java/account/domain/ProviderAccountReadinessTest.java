package account.domain;

import static account.domain.AccountBuilders.KEMAS;
import static account.domain.AccountBuilders.PHONE;
import static account.domain.AccountBuilders.aConfirmedPhoneVerification;
import static account.domain.AccountBuilders.aProvider;
import static account.domain.AccountBuilders.anUnconfirmedPhoneVerification;
import static account.domain.AccountBuilders.kemasVerifiedUntil;
import static account.domain.AccountBuilders.on;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.PhoneNotVerifiedException;
import account.domain.exception.VerificationDetailsRequiredException;
import account.domain.exception.VerificationResultNotFinalException;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

/** Feature: A business provider is verified by the KYC provider. */
class ProviderAccountReadinessTest {

    @Nested
    class Registering {

        @Test
        void aBusinessRegistersWithAConfirmedPhone() {
            ProviderAccount provider = ProviderAccount.register(aConfirmedPhoneVerification());

            assertThat(provider.phone()).isEqualTo(PHONE);
            assertThat(provider.id()).isNotBlank();
        }

        @Test
        void aProviderCanOnlyRegisterWithAConfirmedPhoneNumber() {
            assertThatThrownBy(() -> ProviderAccount.register(anUnconfirmedPhoneVerification()))
                    .isInstanceOf(PhoneNotVerifiedException.class);
        }

        @Test
        void aNewBusinessIsUnverifiedWithNoPayoutAccount() {
            ProviderAccount provider = aProvider();

            assertThat(provider.verificationStatus()).isEqualTo(VerificationStatus.PENDING);
            assertThat(provider.businessRegistration()).isEmpty();
            assertThat(provider.verifiedUntil()).isEmpty();
            assertThat(provider.lastVerifiedAt()).isEmpty();
            assertThat(provider.isPayoutAccountReady()).isFalse();
            assertThat(provider.isReadyForGoLive()).isFalse();
        }
    }

    @Nested
    class Verifying {

        @Test
        void aBusinessIsVerifiedThroughKyb() {
            ProviderAccount provider = aProvider();

            VerificationOutcome outcome = provider.recordKybResult(kemasVerifiedUntil("1 April"), on("1 January"));

            assertThat(outcome).isEqualTo(VerificationOutcome.VERIFIED);
            assertThat(provider.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(provider.businessRegistration()).contains(KEMAS);
            assertThat(provider.verifiedUntil()).contains(on("1 April"));
            assertThat(provider.lastVerifiedAt()).contains(on("1 January"));
        }

        @Test
        void aRejectedKybCanBeRedone() {
            ProviderAccount provider = aProvider();

            assertThat(provider.recordKybResult(KybResult.rejected(), on("1 January"))).isEqualTo(VerificationOutcome.REJECTED);
            assertThat(provider.verificationStatus()).isEqualTo(VerificationStatus.REJECTED);
            assertThat(provider.recordKybResult(KybResult.rejected(), on("2 January"))).isEqualTo(VerificationOutcome.REJECTED);

            assertThat(provider.recordKybResult(kemasVerifiedUntil("5 April"), on("5 January"))).isEqualTo(VerificationOutcome.VERIFIED);
            assertThat(provider.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        }

        @ParameterizedTest(name = "{0} is not a result")
        @NullSource
        @EnumSource(value = VerificationStatus.class, names = {"PENDING", "LAPSED"})
        void onlyVerifiedOrRejectedIsAResult(VerificationStatus status) {
            assertThatThrownBy(() -> new KybResult(status, Optional.empty(), Optional.empty()))
                    .isInstanceOf(VerificationResultNotFinalException.class);
            assertThatThrownBy(() -> new KycResult(status, Optional.empty(), Optional.empty()))
                    .isInstanceOf(VerificationResultNotFinalException.class);
        }

        @Test
        void aVerifiedKybCarriesTheRegistrationAndTheDate() {
            assertThatThrownBy(() -> KybResult.verified(null, on("1 April")))
                    .isInstanceOf(VerificationDetailsRequiredException.class)
                    .hasMessageContaining("business registration");
            assertThatThrownBy(() -> KybResult.verified(KEMAS, null))
                    .isInstanceOf(VerificationDetailsRequiredException.class)
                    .hasMessageContaining("verified-until");
            assertThatThrownBy(() -> new KybResult(VerificationStatus.REJECTED, null, Optional.empty()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new KybResult(VerificationStatus.REJECTED, Optional.empty(), null))
                    .isInstanceOf(NullPointerException.class);
            assertThat(KybResult.rejected().isVerified()).isFalse();
            assertThat(kemasVerifiedUntil("1 April").isVerified()).isTrue();
        }

        @Test
        void aRegistrationHasAnSsmNumberAndARegisteredName() {
            assertThatThrownBy(() -> new BusinessRegistration(" ", "Kemas Home Care Sdn Bhd"))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessageContaining("SSM");
            assertThatThrownBy(() -> new BusinessRegistration("202601012345", " "))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessageContaining("business name");
            assertThat(new BusinessRegistration(" 202601012345 ", " Kemas ").ssmNumber()).isEqualTo("202601012345");
        }
    }

    @Nested
    class GoLiveReadiness {

        @ParameterizedTest(name = "verification {0} and payout ready {1}: ready for go-live is {2}")
        @CsvSource({
            "VERIFIED, true,  true",
            "VERIFIED, false, false",
            "PENDING,  true,  false",
            "REJECTED, true,  false",
            "LAPSED,   true,  false"
        })
        void aBusinessIsReadyWhenVerifiedAndPayoutReady(VerificationStatus status, boolean payout, boolean ready) {
            ProviderAccount provider = aProvider();
            if (status == VerificationStatus.REJECTED) {
                provider.recordKybResult(KybResult.rejected(), on("1 January"));
            }
            if (status == VerificationStatus.VERIFIED || status == VerificationStatus.LAPSED) {
                provider.recordKybResult(kemasVerifiedUntil("1 April"), on("1 January"));
            }
            if (status == VerificationStatus.LAPSED) {
                provider.lapse();
            }
            if (payout) {
                provider.recordPayoutAccountReady();
            }

            assertThat(provider.verificationStatus()).isEqualTo(status);
            assertThat(provider.isPayoutAccountReady()).isEqualTo(payout);
            assertThat(provider.isReadyForGoLive()).isEqualTo(ready);
        }
    }

    @Test
    void aRepeatedPayoutAccountReportChangesNothing() {
        ProviderAccount provider = aProvider();

        assertThat(provider.recordPayoutAccountReady()).isTrue();
        assertThat(provider.recordPayoutAccountReady()).isFalse();
        assertThat(provider.isPayoutAccountReady()).isTrue();
    }
}
