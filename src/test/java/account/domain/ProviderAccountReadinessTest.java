package account.domain;

import static account.domain.AccountBuilders.LIMITS;
import static account.domain.AccountBuilders.PHONE;
import static account.domain.AccountBuilders.aConfirmedPhoneVerification;
import static account.domain.AccountBuilders.aProvider;
import static account.domain.AccountBuilders.anUnconfirmedPhoneVerification;
import static account.domain.AccountBuilders.on;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.EkycResultNotFinalException;
import account.domain.exception.PhoneNotVerifiedException;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

/** Feature: Provider readiness. */
class ProviderAccountReadinessTest {

    @Nested
    class Registering {

        @Test
        void aProviderRegistersWithAConfirmedPhoneAndTheirLegalName() {
            ProviderAccount provider = ProviderAccount.register(aConfirmedPhoneVerification(), " Nur Aina binti Ahmad ");

            assertThat(provider.phone()).isEqualTo(PHONE);
            assertThat(provider.legalName()).isEqualTo("Nur Aina binti Ahmad");
            assertThat(provider.id()).isNotBlank();
        }

        @Test
        void aProviderCanOnlyRegisterWithAConfirmedPhoneNumber() {
            assertThatThrownBy(() -> ProviderAccount.register(anUnconfirmedPhoneVerification(), "Nur Aina"))
                    .isInstanceOf(PhoneNotVerifiedException.class);
        }

        @Test
        void aProviderNeedsALegalName() {
            assertThatThrownBy(() -> ProviderAccount.register(aConfirmedPhoneVerification(), ""))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessage("A provider's legal name is required");
        }

        @Test
        void aNewProviderIsUnverifiedWithNoPayoutAccountAndIsTheirOwnHelper() {
            ProviderAccount provider = aProvider();

            assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.PENDING);
            assertThat(provider.isPayoutAccountReady()).isFalse();
            assertThat(provider.includesSelfAsHelper()).isTrue();
            assertThat(provider.isReadyForGoLive()).isFalse();
        }
    }

    @Nested
    class Verifying {

        @Test
        void kenalsResultIsRecorded() {
            ProviderAccount provider = aProvider();

            EkycOutcome outcome = provider.recordEkycResult(VerificationStatus.VERIFIED, LIMITS, on("1 January"));

            assertThat(outcome).isEqualTo(EkycOutcome.VERIFIED);
            assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(provider.verification()).contains(
                    new Verification(on("1 January"), on("1 April"), false, Optional.empty()));
        }

        @Test
        void aRejectedProviderCanBeVerifiedOnALaterAttempt() {
            ProviderAccount provider = aProvider();
            assertThat(provider.recordEkycResult(VerificationStatus.REJECTED, LIMITS, on("1 January"))).isEqualTo(EkycOutcome.REJECTED);
            assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.REJECTED);
            assertThat(provider.verification()).isEmpty();

            assertThat(provider.recordEkycResult(VerificationStatus.VERIFIED, LIMITS, on("5 January")))
                    .isEqualTo(EkycOutcome.VERIFIED);

            assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(provider.verification().orElseThrow().dueAt()).isEqualTo(on("5 April"));
        }

        @ParameterizedTest(name = "{0} is not an eKYC result")
        @NullSource
        @EnumSource(value = VerificationStatus.class, names = {"PENDING", "LAPSED"})
        void pendingIsNotAnEkycResult(VerificationStatus result) {
            ProviderAccount provider = aProvider();
            provider.recordEkycResult(VerificationStatus.VERIFIED, LIMITS, on("1 January"));

            assertThatThrownBy(() -> provider.recordEkycResult(result, LIMITS, on("1 January"))).isInstanceOf(EkycResultNotFinalException.class);
            assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
        }
    }

    @Nested
    class GoLiveReadiness {

        @ParameterizedTest(name = "eKYC {0} and payout ready {1}: ready for go-live is {2}")
        @CsvSource({
            "VERIFIED, true,  true",
            "VERIFIED, false, false",
            "PENDING,  true,  false",
            "REJECTED, true,  false"
        })
        void aProviderIsReadyForGoLiveOnlyWhenVerifiedAndPayoutReady(VerificationStatus ekyc, boolean payout, boolean ready) {
            ProviderAccount provider = aProvider();
            if (ekyc != VerificationStatus.PENDING) {
                provider.recordEkycResult(ekyc, LIMITS, on("1 January"));
            }
            if (payout) {
                provider.recordPayoutAccountReady();
            }

            assertThat(provider.isPayoutAccountReady()).isEqualTo(payout);
            assertThat(provider.isReadyForGoLive()).isEqualTo(ready);
        }
    }

    @Nested
    class RunningATeam {

        @Test
        void aProviderWhoRunsATeamCanChooseNotToBeAHelper() {
            ProviderAccount provider = aProvider();

            provider.includeSelfAsHelper(false);
            assertThat(provider.includesSelfAsHelper()).isFalse();

            provider.includeSelfAsHelper(true);
            assertThat(provider.includesSelfAsHelper()).isTrue();
        }
    }
}
