package account.domain;

import static account.domain.AccountBuilders.ACCEPTED;
import static account.domain.AccountBuilders.INDONESIA;
import static account.domain.AccountBuilders.LIMITS;
import static account.domain.AccountBuilders.PHONE;
import static account.domain.AccountBuilders.aBusinessProvider;
import static account.domain.AccountBuilders.aConfirmedPhoneVerification;
import static account.domain.AccountBuilders.aMyKad;
import static account.domain.AccountBuilders.aProvider;
import static account.domain.AccountBuilders.anUnconfirmedPhoneVerification;
import static account.domain.AccountBuilders.on;
import static account.domain.AccountBuilders.verifiedWith;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.AccountDetailRequiredException;
import account.domain.exception.BusinessRegistrationAlreadyCheckedException;
import account.domain.exception.DocumentNotAcceptedException;
import account.domain.exception.EkycDocumentRequiredException;
import account.domain.exception.EkycResultNotFinalException;
import account.domain.exception.NotABusinessProviderException;
import account.domain.exception.PhoneNotVerifiedException;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

/** Feature: Provider readiness, with provider types and documents. */
class ProviderAccountReadinessTest {

    private static final EkycResult NUR_AINA = verifiedWith(aMyKad("Nur Aina binti Ahmad"));

    @Nested
    class Registering {

        @Test
        void aProviderRegistersWithAConfirmedPhoneAndTheirType() {
            ProviderAccount provider = ProviderAccount.register(aConfirmedPhoneVerification(), ProviderType.BUSINESS);

            assertThat(provider.phone()).isEqualTo(PHONE);
            assertThat(provider.type()).isEqualTo(ProviderType.BUSINESS);
            assertThat(provider.id()).isNotBlank();
        }

        @Test
        void aProviderCanOnlyRegisterWithAConfirmedPhoneNumber() {
            assertThatThrownBy(() -> ProviderAccount.register(anUnconfirmedPhoneVerification(), ProviderType.INDEPENDENT))
                    .isInstanceOf(PhoneNotVerifiedException.class);
        }

        @Test
        void aProviderChoosesAType() {
            assertThatThrownBy(() -> ProviderAccount.register(aConfirmedPhoneVerification(), null))
                    .isInstanceOf(NullPointerException.class);
        }

        @Test
        void aNewProviderIsUnverifiedWithNoPayoutAccountAndIsTheirOwnHelper() {
            ProviderAccount provider = aProvider();

            assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.PENDING);
            assertThat(provider.registeredName()).isEmpty();
            assertThat(provider.document()).isEmpty();
            assertThat(provider.verification()).isEmpty();
            assertThat(provider.businessRegistration()).isEmpty();
            assertThat(provider.isPayoutAccountReady()).isFalse();
            assertThat(provider.includesSelfAsHelper()).isTrue();
            assertThat(provider.isReadyForGoLive()).isFalse();
        }
    }

    @Nested
    class Verifying {

        @Test
        void kenalsResultIsRecordedWithTheRegisteredName() {
            ProviderAccount provider = aProvider();

            EkycOutcome outcome = provider.recordEkycResult(NUR_AINA, ACCEPTED, LIMITS, on("1 January"));

            assertThat(outcome).isEqualTo(EkycOutcome.VERIFIED);
            assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(provider.registeredName()).contains("Nur Aina binti Ahmad");
            assertThat(provider.document()).contains(aMyKad("Nur Aina binti Ahmad"));
            assertThat(provider.verification()).contains(
                    new Verification(on("1 January"), on("1 April"), false, Optional.empty()));
        }

        @Test
        void aRejectedProviderCanBeVerifiedOnALaterAttempt() {
            ProviderAccount provider = aProvider();
            assertThat(provider.recordEkycResult(EkycResult.rejected(), ACCEPTED, LIMITS, on("1 January")))
                    .isEqualTo(EkycOutcome.REJECTED);
            assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.REJECTED);
            assertThat(provider.verification()).isEmpty();

            assertThat(provider.recordEkycResult(NUR_AINA, ACCEPTED, LIMITS, on("5 January"))).isEqualTo(EkycOutcome.VERIFIED);

            assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
            assertThat(provider.verification().orElseThrow().dueAt()).isEqualTo(on("5 April"));
        }

        @ParameterizedTest(name = "a {0} issued by MY is accepted for a provider")
        @EnumSource(DocumentType.class)
        void theProviderVerifiesWithAMyKadOrAMalaysianPassport(DocumentType type) {
            ProviderAccount provider = aProvider();

            provider.recordEkycResult(
                    verifiedWith(new IdentityDocument(Country.MALAYSIA, type, "Nur Aina binti Ahmad")), ACCEPTED, LIMITS, on("1 January"));

            assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
        }

        @Test
        void theProviderMustBeMalaysian() {
            ProviderAccount provider = aProvider();
            EkycResult indonesianPassport =
                    verifiedWith(new IdentityDocument(INDONESIA, DocumentType.PASSPORT, "Nur Aina binti Ahmad"));

            assertThatThrownBy(() -> provider.recordEkycResult(indonesianPassport, ACCEPTED, LIMITS, on("1 January")))
                    .isInstanceOf(DocumentNotAcceptedException.class)
                    .hasMessageContaining("PROVIDER")
                    .hasMessageContaining("PASSPORT");
            assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.PENDING);
            assertThat(provider.registeredName()).isEmpty();
        }

        @ParameterizedTest(name = "{0} is not an eKYC result")
        @NullSource
        @EnumSource(value = VerificationStatus.class, names = {"PENDING", "LAPSED"})
        void onlyVerifiedOrRejectedIsAnEkycResult(VerificationStatus status) {
            assertThatThrownBy(() -> new EkycResult(status, Optional.empty())).isInstanceOf(EkycResultNotFinalException.class);
        }

        @Test
        void aVerifiedResultCarriesItsDocument() {
            assertThatThrownBy(() -> new EkycResult(VerificationStatus.VERIFIED, Optional.empty()))
                    .isInstanceOf(EkycDocumentRequiredException.class);
            assertThatThrownBy(() -> new EkycResult(VerificationStatus.REJECTED, null)).isInstanceOf(NullPointerException.class);
            assertThat(EkycResult.rejected().isVerified()).isFalse();
            assertThat(NUR_AINA.isVerified()).isTrue();
            assertThat(NUR_AINA.document()).contains(aMyKad("Nur Aina binti Ahmad"));
        }

        @Test
        void aDocumentHasARegisteredNameIssuingCountryAndType() {
            assertThatThrownBy(() -> new IdentityDocument(Country.MALAYSIA, DocumentType.PASSPORT, " "))
                    .isInstanceOf(AccountDetailRequiredException.class)
                    .hasMessage("A registered name is required");
            assertThatThrownBy(() -> new IdentityDocument(null, DocumentType.PASSPORT, "Aina"))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new IdentityDocument(Country.MALAYSIA, null, "Aina"))
                    .isInstanceOf(NullPointerException.class);
            assertThat(new IdentityDocument(Country.MALAYSIA, DocumentType.PASSPORT, " Aina ").registeredName()).isEqualTo("Aina");
        }
    }

    @Nested
    class BusinessRegistrationCheck {

        @Test
        void homaChecksABusinessRegistrationOnce() {
            ProviderAccount business = aBusinessProvider();

            business.recordBusinessRegistrationChecked(" 202601012345 ", on("2 January"));

            assertThat(business.businessRegistration()).contains(new BusinessRegistration("202601012345", on("2 January")));
            assertThatThrownBy(() -> business.recordBusinessRegistrationChecked("202601012345", on("3 January")))
                    .isInstanceOf(BusinessRegistrationAlreadyCheckedException.class)
                    .hasMessageContaining(business.id());
        }

        @Test
        void anIndependentProviderHasNoBusinessRegistration() {
            ProviderAccount independent = aProvider();

            assertThatThrownBy(() -> independent.recordBusinessRegistrationChecked("202601012345", on("2 January")))
                    .isInstanceOf(NotABusinessProviderException.class)
                    .hasMessageContaining(independent.id());
            assertThat(independent.businessRegistration()).isEmpty();
        }

        @Test
        void aRegistrationHasANumberAndACheckTime() {
            assertThatThrownBy(() -> new BusinessRegistration(" ", on("2 January")))
                    .isInstanceOf(AccountDetailRequiredException.class);
            assertThatThrownBy(() -> new BusinessRegistration("202601012345", null)).isInstanceOf(NullPointerException.class);
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
        void anIndependentProviderIsReadyWhenVerifiedAndPayoutReady(VerificationStatus ekyc, boolean payout, boolean ready) {
            ProviderAccount provider = aProvider();
            if (ekyc == VerificationStatus.VERIFIED) {
                provider.recordEkycResult(NUR_AINA, ACCEPTED, LIMITS, on("1 January"));
            }
            if (ekyc == VerificationStatus.REJECTED) {
                provider.recordEkycResult(EkycResult.rejected(), ACCEPTED, LIMITS, on("1 January"));
            }
            if (payout) {
                provider.recordPayoutAccountReady();
            }

            assertThat(provider.isPayoutAccountReady()).isEqualTo(payout);
            assertThat(provider.isReadyForGoLive()).isEqualTo(ready);
        }

        @Test
        void aBusinessCannotGoLiveBeforeItsRegistrationIsChecked() {
            ProviderAccount business = aBusinessProvider();
            business.recordEkycResult(NUR_AINA, ACCEPTED, LIMITS, on("1 January"));
            business.recordPayoutAccountReady();

            assertThat(business.isReadyForGoLive()).isFalse();

            business.recordBusinessRegistrationChecked("202601012345", on("2 January"));

            assertThat(business.isReadyForGoLive()).isTrue();
        }
    }

    @Nested
    class RunningATeam {

        @Test
        void aProviderCanRemoveThemselvesFromTheirTeam() {
            ProviderAccount provider = aProvider();

            provider.includeSelfAsHelper(false);
            assertThat(provider.includesSelfAsHelper()).isFalse();

            provider.includeSelfAsHelper(true);
            assertThat(provider.includesSelfAsHelper()).isTrue();
        }
    }
}
