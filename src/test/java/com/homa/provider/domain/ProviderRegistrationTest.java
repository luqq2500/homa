package com.homa.provider.domain;

import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_NAME;
import static com.homa.fixtures.HomaFixtures.KEMAS_HOME_REGISTRATION_NO;
import static com.homa.provider.domain.ProviderFixtures.BUSINESS_VERIFIED_AT;
import static com.homa.provider.domain.ProviderFixtures.KEMAS_HOME;
import static com.homa.provider.domain.ProviderFixtures.kemasHomeRegistration;
import static com.homa.provider.domain.ProviderFixtures.liveKemasHome;
import static com.homa.provider.domain.ProviderFixtures.registeredKemasHome;
import static com.homa.provider.domain.ProviderFixtures.siti;
import static com.homa.provider.domain.ProviderFixtures.verifiedKemasHome;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.homa.provider.domain.exception.BusinessRegistrationRequiredException;
import com.homa.provider.domain.exception.BusinessVerificationAlreadyRecordedException;
import com.homa.provider.domain.exception.BusinessVerificationNotFailedException;
import com.homa.provider.domain.exception.ProviderNotVerifiedException;
import com.homa.provider.domain.exception.ProviderRuleViolationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

class ProviderRegistrationTest {

    @Test
    @Tag("PRV-1")
    @DisplayName("Kemas Home submits its company registration. Once it's verified, Kemas Home can continue onboarding.")
    void providerContinuesOnboardingOnceItsBusinessIsVerified() {
        Provider kemasHome = registeredKemasHome();

        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.REGISTERED);
        assertThat(kemasHome.businessVerifiedAt()).isEmpty();
        assertThatThrownBy(() -> siti(kemasHome)).isInstanceOf(ProviderNotVerifiedException.class)
                .hasMessageContaining(KEMAS_HOME.value());

        kemasHome.recordBusinessVerification(VerificationOutcome.PASSED, BUSINESS_VERIFIED_AT);

        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.VERIFIED);
        assertThat(kemasHome.businessVerifiedAt()).contains(BUSINESS_VERIFIED_AT);
        assertThat(siti(kemasHome).providerId()).isEqualTo(KEMAS_HOME);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A sole trader without registration can't join.")
    void soleTraderWithoutRegistrationCannotJoin() {
        assertThatThrownBy(() -> new BusinessRegistration(null, "Sofia Cleans"))
                .isInstanceOf(BusinessRegistrationRequiredException.class)
                .isInstanceOf(ProviderRuleViolationException.class);
        assertThatThrownBy(() -> new BusinessRegistration("", "Sofia Cleans"))
                .isInstanceOf(BusinessRegistrationRequiredException.class);
        assertThatThrownBy(() -> new BusinessRegistration("   ", "Sofia Cleans"))
                .isInstanceOf(BusinessRegistrationRequiredException.class);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A business that fails business verification can't add its team or go live.")
    void providerCannotContinueOnboardingWhenItsBusinessFailsVerification() {
        Provider kemasHome = registeredKemasHome();

        kemasHome.recordBusinessVerification(VerificationOutcome.FAILED, BUSINESS_VERIFIED_AT);

        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.VERIFICATION_FAILED);
        assertThat(kemasHome.businessVerifiedAt()).isEmpty();
        assertThatThrownBy(() -> siti(kemasHome)).isInstanceOf(ProviderNotVerifiedException.class);
        assertThatThrownBy(() -> kemasHome.goLive(3)).isInstanceOf(ProviderNotVerifiedException.class);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A business verification result is recorded once.")
    void businessVerificationIsRecordedOnlyOnce() {
        Provider verified = verifiedKemasHome();
        Provider failed = registeredKemasHome();
        failed.recordBusinessVerification(VerificationOutcome.FAILED, BUSINESS_VERIFIED_AT);

        assertThatThrownBy(() -> verified.recordBusinessVerification(VerificationOutcome.FAILED, BUSINESS_VERIFIED_AT))
                .isInstanceOf(BusinessVerificationAlreadyRecordedException.class)
                .hasMessageContaining(KEMAS_HOME.value());
        assertThatThrownBy(() -> failed.recordBusinessVerification(VerificationOutcome.PASSED, BUSINESS_VERIFIED_AT))
                .isInstanceOf(BusinessVerificationAlreadyRecordedException.class);
        assertThat(verified.status()).isEqualTo(ProviderStatus.VERIFIED);
        assertThat(failed.status()).isEqualTo(ProviderStatus.VERIFICATION_FAILED);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Kemas Home's business verification fails because its registration number was mistyped. It resubmits"
            + " with the corrected number, passes, and continues onboarding.")
    void businessThatFailsVerificationCanResubmitAndContinueOnceItPasses() {
        Provider kemasHome = registeredKemasHome();
        kemasHome.recordBusinessVerification(VerificationOutcome.FAILED, BUSINESS_VERIFIED_AT);
        BusinessRegistration corrected = new BusinessRegistration("202301012346", KEMAS_HOME_NAME);

        kemasHome.resubmitForVerification(corrected);

        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.REGISTERED);
        assertThat(kemasHome.registration()).isEqualTo(corrected);
        assertThatThrownBy(() -> siti(kemasHome)).isInstanceOf(ProviderNotVerifiedException.class);

        kemasHome.recordBusinessVerification(VerificationOutcome.PASSED, BUSINESS_VERIFIED_AT.plusSeconds(3600));

        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.VERIFIED);
        assertThat(kemasHome.businessVerifiedAt()).contains(BUSINESS_VERIFIED_AT.plusSeconds(3600));
        assertThat(siti(kemasHome).isActive()).isTrue();
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("A business can fail and resubmit as often as it needs; each submission gets one result.")
    void businessCanResubmitAfterEachFailure() {
        Provider kemasHome = registeredKemasHome();
        kemasHome.recordBusinessVerification(VerificationOutcome.FAILED, BUSINESS_VERIFIED_AT);
        kemasHome.resubmitForVerification(kemasHomeRegistration());
        kemasHome.recordBusinessVerification(VerificationOutcome.FAILED, BUSINESS_VERIFIED_AT);

        kemasHome.resubmitForVerification(kemasHomeRegistration());

        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.REGISTERED);
        assertThatThrownBy(() -> kemasHome.resubmitForVerification(kemasHomeRegistration()))
                .isInstanceOf(BusinessVerificationNotFailedException.class)
                .hasMessageContaining(KEMAS_HOME.value());
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Only a business whose verification failed resubmits; a verified or live business has nothing to retry.")
    void onlyAFailedBusinessVerificationIsResubmitted() {
        Provider verified = verifiedKemasHome();
        Provider live = liveKemasHome();

        assertThatThrownBy(() -> verified.resubmitForVerification(kemasHomeRegistration()))
                .isInstanceOf(BusinessVerificationNotFailedException.class);
        assertThatThrownBy(() -> live.resubmitForVerification(kemasHomeRegistration()))
                .isInstanceOf(BusinessVerificationNotFailedException.class);
        assertThatThrownBy(() -> verified.resubmitForVerification(null)).isInstanceOf(NullPointerException.class);
        assertThat(verified.status()).isEqualTo(ProviderStatus.VERIFIED);
        assertThat(live.status()).isEqualTo(ProviderStatus.LIVE);
    }

    @Test
    @Tag("PRV-1")
    @DisplayName("Kemas Home registers with its company registration number and registered name.")
    void providerRegistersWithItsBusinessRegistration() {
        Provider kemasHome = registeredKemasHome();

        assertThat(kemasHome.id()).isEqualTo(KEMAS_HOME);
        assertThat(kemasHome.registration().registrationNumber()).isEqualTo(KEMAS_HOME_REGISTRATION_NO);
        assertThat(kemasHome.registration().businessName()).isEqualTo(KEMAS_HOME_NAME);
        assertThat(kemasHome.isLive()).isFalse();
    }

    @Test
    @DisplayName("A business registration needs the registered business name.")
    void businessRegistrationNeedsTheRegisteredName() {
        assertThatThrownBy(() -> new BusinessRegistration(KEMAS_HOME_REGISTRATION_NO, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new BusinessRegistration(KEMAS_HOME_REGISTRATION_NO, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("A stored provider is rebuilt as it was saved.")
    void restoredProviderKeepsItsStoredState() {
        Provider kemasHome = Provider.restore(KEMAS_HOME, kemasHomeRegistration(), ProviderStatus.LIVE,
                BUSINESS_VERIFIED_AT);

        assertThat(kemasHome.id()).isEqualTo(KEMAS_HOME);
        assertThat(kemasHome.registration()).isEqualTo(kemasHomeRegistration());
        assertThat(kemasHome.status()).isEqualTo(ProviderStatus.LIVE);
        assertThat(kemasHome.isLive()).isTrue();
        assertThat(kemasHome.businessVerifiedAt()).contains(BUSINESS_VERIFIED_AT);
    }
}
