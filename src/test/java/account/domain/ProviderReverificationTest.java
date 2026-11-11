package account.domain;

import static account.domain.AccountBuilders.aProvider;
import static account.domain.AccountBuilders.aVerifiedProvider;
import static account.domain.AccountBuilders.kemasVerifiedUntil;
import static account.domain.AccountBuilders.on;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.ProviderNotVerifiedException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Feature: The KYC provider re-verifies a business. Kemas was verified on 1 January, until 1 April, with a ready payout account. */
class ProviderReverificationTest {

    private ProviderAccount provider;

    @BeforeEach
    void kemasIsVerifiedUntilTheFirstOfApril() {
        provider = aVerifiedProvider();
        provider.recordPayoutAccountReady();
    }

    @Test
    void aPassedReverificationKeepsTheBusinessVerifiedUntilTheNewDate() {
        VerificationOutcome outcome = provider.recordKybResult(kemasVerifiedUntil("20 June"), on("20 March"));

        assertThat(outcome).isEqualTo(VerificationOutcome.REVERIFIED);
        assertThat(provider.verifiedUntil()).contains(on("20 June"));
        assertThat(provider.lastVerifiedAt()).contains(on("20 March"));
        assertThat(provider.isReadyForGoLive()).isTrue();
    }

    @Test
    void aRepeatedPassChangesNothing() {
        VerificationOutcome outcome = provider.recordKybResult(kemasVerifiedUntil("1 April"), on("2 January"));

        assertThat(outcome).isEqualTo(VerificationOutcome.UNCHANGED);
        assertThat(provider.lastVerifiedAt()).contains(on("1 January"));
    }

    @Test
    void aFailedReverificationChangesNothing() {
        VerificationOutcome outcome = provider.recordKybResult(KybResult.rejected(), on("20 March"));

        assertThat(outcome).isEqualTo(VerificationOutcome.RECHECK_FAILED);
        assertThat(provider.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(provider.verifiedUntil()).contains(on("1 April"));
        assertThat(provider.isReadyForGoLive()).isTrue();
    }

    @Test
    void aLapsedBusinessIsNotReadyAndTakesNoNewRequests() {
        provider.lapse();

        assertThat(provider.verificationStatus()).isEqualTo(VerificationStatus.LAPSED);
        assertThat(provider.isReadyForGoLive()).isFalse();
    }

    @Test
    void aLapsedBusinessThatPassesIsVerifiedAgain() {
        provider.lapse();

        VerificationOutcome outcome = provider.recordKybResult(kemasVerifiedUntil("5 July"), on("5 April"));

        assertThat(outcome).isEqualTo(VerificationOutcome.REVERIFIED);
        assertThat(provider.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(provider.isReadyForGoLive()).isTrue();
    }

    @Test
    void aLapsedBusinessThatPassesWithTheSameDateIsVerifiedAgain() {
        provider.lapse();

        assertThat(provider.recordKybResult(kemasVerifiedUntil("1 April"), on("5 April"))).isEqualTo(VerificationOutcome.REVERIFIED);
        assertThat(provider.verificationStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    void aLapsedBusinessThatFailsStaysLapsed() {
        provider.lapse();

        assertThat(provider.recordKybResult(KybResult.rejected(), on("5 April"))).isEqualTo(VerificationOutcome.RECHECK_FAILED);
        assertThat(provider.verificationStatus()).isEqualTo(VerificationStatus.LAPSED);
    }

    @ParameterizedTest(name = "a {0} business cannot lapse")
    @EnumSource(value = VerificationStatus.class, names = {"PENDING", "REJECTED", "LAPSED"})
    void onlyAVerifiedBusinessLapses(VerificationStatus status) {
        ProviderAccount notVerified = status == VerificationStatus.LAPSED ? provider : aProvider();
        if (status == VerificationStatus.REJECTED) {
            notVerified.recordKybResult(KybResult.rejected(), on("1 January"));
        }
        if (status == VerificationStatus.LAPSED) {
            notVerified.lapse();
        }

        assertThatThrownBy(notVerified::lapse)
                .isInstanceOf(ProviderNotVerifiedException.class)
                .hasMessageContaining(notVerified.id())
                .hasMessageContaining(status.name());
        assertThat(notVerified.verificationStatus()).isEqualTo(status);
    }
}
