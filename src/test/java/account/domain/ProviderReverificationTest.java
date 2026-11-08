package account.domain;

import static account.domain.AccountBuilders.LIMITS;
import static account.domain.AccountBuilders.aProvider;
import static account.domain.AccountBuilders.aProviderVerifiedOn;
import static account.domain.AccountBuilders.on;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.ProviderAlreadyVerifiedException;
import account.domain.exception.ProviderNotVerifiedException;
import account.domain.exception.RecheckNotYetDueException;
import account.domain.exception.ReverificationNotYetDueException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Feature: Provider re-verification. The provider was verified on 1 January and their payout account is ready. */
class ProviderReverificationTest {

    private ProviderAccount provider;

    @BeforeEach
    void aLiveReadyProviderVerifiedOnTheFirstOfJanuary() {
        provider = aProviderVerifiedOn("1 January");
        provider.recordPayoutAccountReady();
    }

    private Verification verification() {
        return provider.verification().orElseThrow();
    }

    @Test
    void aProvidersReverificationIsDueThreeMonthsAfterTheirLastVerification() {
        assertThat(verification()).isEqualTo(new Verification(on("1 January"), on("1 April"), false, Optional.empty()));
        assertThat(provider.isReadyForGoLive()).isTrue();
    }

    @Test
    void aProvidersRecheckOpensFourteenDaysBeforeTheDueDate() {
        assertThatThrownBy(() -> provider.openRecheck(LIMITS, on("17 March"))).isInstanceOf(RecheckNotYetDueException.class);

        provider.openRecheck(LIMITS, on("18 March"));

        assertThat(verification().recheckOpen()).isTrue();
    }

    @Test
    void passingTheRecheckResetsTheDueDate() {
        provider.openRecheck(LIMITS, on("18 March"));

        EkycOutcome outcome = provider.recordEkycResult(VerificationStatus.VERIFIED, LIMITS, on("20 March"));

        assertThat(outcome).isEqualTo(EkycOutcome.REVERIFIED);
        assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(verification()).isEqualTo(new Verification(on("20 March"), on("20 June"), false, Optional.empty()));
    }

    @Test
    void aFailedRecheckChangesNothingYet() {
        provider.openRecheck(LIMITS, on("18 March"));

        EkycOutcome outcome = provider.recordEkycResult(VerificationStatus.REJECTED, LIMITS, on("20 March"));

        assertThat(outcome).isEqualTo(EkycOutcome.RECHECK_FAILED);
        assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(provider.isReadyForGoLive()).isTrue();
        assertThat(verification().recheckOpen()).isTrue();
    }

    @Test
    void aRepeatedVerifiedResultWithNoRecheckOpenChangesNothing() {
        EkycOutcome outcome = provider.recordEkycResult(VerificationStatus.VERIFIED, LIMITS, on("2 January"));

        assertThat(outcome).isEqualTo(EkycOutcome.UNCHANGED);
        assertThat(verification().lastVerified()).isEqualTo(on("1 January"));
    }

    @Test
    void aRejectedResultWithNoRecheckOpenIsRefused() {
        assertThatThrownBy(() -> provider.recordEkycResult(VerificationStatus.REJECTED, LIMITS, on("2 January")))
                .isInstanceOf(ProviderAlreadyVerifiedException.class)
                .hasMessageContaining(provider.id());
        assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    void aLapsedProviderIsNotReadyAndTakesNoNewRequests() {
        provider.lapse(on("1 April"));

        assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.LAPSED);
        assertThat(provider.isReadyForGoLive()).isFalse();
    }

    @Test
    void aProviderDoesNotLapseBeforeTheDueDate() {
        assertThatThrownBy(() -> provider.lapse(on("31 March")))
                .isInstanceOf(ReverificationNotYetDueException.class);
        assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
    }

    @Test
    void aLapsedProviderWhoPassesIsVerifiedAgain() {
        provider.lapse(on("1 April"));

        EkycOutcome outcome = provider.recordEkycResult(VerificationStatus.VERIFIED, LIMITS, on("5 April"));

        assertThat(outcome).isEqualTo(EkycOutcome.REVERIFIED);
        assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.VERIFIED);
        assertThat(provider.isReadyForGoLive()).isTrue();
        assertThat(verification().dueAt()).isEqualTo(on("5 July"));
    }

    @Test
    void aLapsedProviderWhoFailsStaysLapsed() {
        provider.lapse(on("1 April"));

        EkycOutcome outcome = provider.recordEkycResult(VerificationStatus.REJECTED, LIMITS, on("5 April"));

        assertThat(outcome).isEqualTo(EkycOutcome.RECHECK_FAILED);
        assertThat(provider.ekycStatus()).isEqualTo(VerificationStatus.LAPSED);
    }

    @Test
    void anEarlyRecheckMovesTheProvidersDueDateSooner() {
        provider.requestEarlyRecheck("Report R12: different person attended", LIMITS, on("10 January"));

        assertThat(verification().recheckOpen()).isTrue();
        assertThat(verification().dueAt()).isEqualTo(on("24 January"));
        assertThat(verification().earlyRecheckReason()).contains("Report R12: different person attended");
    }

    @Nested
    class OnlyVerifiedProviders {

        static Stream<Arguments> stepsForProvidersNotVerified() {
            Stream<Named<Consumer<ProviderAccount>>> steps = Stream.of(
                    Named.of("open a re-check", p -> p.openRecheck(LIMITS, on("18 March"))),
                    Named.of("ask for an early re-check", p -> p.requestEarlyRecheck("Report R12", LIMITS, on("18 March"))),
                    Named.of("lapse", p -> p.lapse(on("1 April"))));
            return steps.flatMap(step -> Stream.of(
                    Arguments.of(step, VerificationStatus.PENDING),
                    Arguments.of(step, VerificationStatus.REJECTED),
                    Arguments.of(step, VerificationStatus.LAPSED)));
        }

        @ParameterizedTest(name = "a {1} provider cannot {0}")
        @MethodSource("stepsForProvidersNotVerified")
        void reverificationIsForVerifiedProvidersOnly(Consumer<ProviderAccount> step, VerificationStatus status) {
            ProviderAccount notVerified = switch (status) {
                case PENDING -> aProvider();
                case REJECTED -> {
                    ProviderAccount rejected = aProvider();
                    rejected.recordEkycResult(VerificationStatus.REJECTED, LIMITS, on("1 January"));
                    yield rejected;
                }
                default -> {
                    provider.lapse(on("1 April"));
                    yield provider;
                }
            };

            assertThatThrownBy(() -> step.accept(notVerified))
                    .isInstanceOf(ProviderNotVerifiedException.class)
                    .hasMessageContaining(notVerified.id())
                    .hasMessageContaining(status.name());
            assertThat(notVerified.ekycStatus()).isEqualTo(status);
        }
    }
}
