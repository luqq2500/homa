package account.domain;

import static account.domain.AccountBuilders.ACCEPTED;
import static account.domain.AccountBuilders.LIMITS;
import static account.domain.AccountBuilders.MALAYSIA;
import static account.domain.AccountBuilders.aMyKad;
import static account.domain.AccountBuilders.aTeamMemberSince;
import static account.domain.AccountBuilders.on;
import static account.domain.AccountBuilders.verifiedAsAina;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import account.domain.exception.TeamMemberAlreadyVerifiedException;
import account.domain.exception.NotATeamMemberException;
import account.domain.exception.TeamMemberRemovedException;
import account.domain.exception.InvalidVerificationLimitsException;
import account.domain.exception.RecheckAlreadyOpenException;
import account.domain.exception.RecheckNotYetDueException;
import account.domain.exception.RecheckReasonRequiredException;
import account.domain.exception.ReverificationNotYetDueException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Feature: Periodic re-verification. Aina became a team member on 1 January. */
class TeamMemberReverificationTest {

    private TeamMember aina;

    @BeforeEach
    void ainaBecameATeamMemberOnTheFirstOfJanuary() {
        aina = aTeamMemberSince("1 January");
    }

    private Verification verification() {
        return aina.verification();
    }

    @Nested
    class DueDate {

        @Test
        void reverificationIsDueThreeMonthsAfterTheLastVerification() {
            assertThat(verification().lastVerified()).isEqualTo(on("1 January"));
            assertThat(verification().dueAt()).isEqualTo(on("1 April"));
            assertThat(verification().recheckOpen()).isFalse();
        }

        @Test
        void monthsAreCountedOnMalaysianDates() {
            Instant earlyMorning = LocalDateTime.of(2027, 11, 30, 7, 0).atZone(MALAYSIA).toInstant();
            MemberVerification siti = MemberVerification.start(
                    "provider-1", ProviderType.INDEPENDENT, TeamRole.HELPER, DocumentType.IDENTITY_CARD, Country.MALAYSIA,
                    Photo.of("photos/siti.jpg"), EmploymentConfirmation.given("2026-11", earlyMorning), ACCEPTED, LIMITS,
                    earlyMorning);

            siti.recordEkycResult(EkycResult.verified(aMyKad("Siti binti Omar")), ACCEPTED, earlyMorning);

            assertThat(TeamMember.add(siti, earlyMorning).verification().dueAt())
                    .isEqualTo(LocalDateTime.of(2028, 2, 29, 7, 0).atZone(MALAYSIA).toInstant());
        }

        @Test
        void theDueDateCountsAsDueFromItsExactMoment() {
            assertThat(verification().isDue(on("1 April"))).isTrue();
            assertThat(verification().isDue(on("1 April").minusSeconds(1))).isFalse();
        }
    }

    @Nested
    class OpeningTheRecheck {

        @Test
        void theRecheckOpensFourteenDaysBeforeTheDueDate() {
            assertThat(verification().recheckOpensAt(LIMITS)).isEqualTo(on("18 March"));

            aina.openRecheck(LIMITS, on("18 March"));

            assertThat(verification().recheckOpen()).isTrue();
            assertThat(verification().dueAt()).isEqualTo(on("1 April"));
            assertThat(verification().earlyRecheckReason()).isEmpty();
        }

        @Test
        void theRecheckDoesNotOpenBeforeTheReminder() {
            assertThatThrownBy(() -> aina.openRecheck(LIMITS, on("17 March")))
                    .isInstanceOf(RecheckNotYetDueException.class)
                    .hasMessageContaining(on("18 March").toString());
            assertThat(verification().recheckOpen()).isFalse();
        }

        @Test
        void anOpenRecheckIsNotOpenedTwice() {
            aina.openRecheck(LIMITS, on("18 March"));

            assertThatThrownBy(() -> aina.openRecheck(LIMITS, on("19 March")))
                    .isInstanceOf(RecheckAlreadyOpenException.class)
                    .hasMessageContaining(aina.id());
        }
    }

    @Nested
    class Results {

        @Test
        void passingTheRecheckResetsTheDueDate() {
            aina.openRecheck(LIMITS, on("18 March"));

            EkycOutcome outcome = aina.recordEkycResult(verifiedAsAina(), ACCEPTED, LIMITS, on("20 March"));

            assertThat(outcome).isEqualTo(EkycOutcome.REVERIFIED);
            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.MEMBER);
            assertThat(verification()).isEqualTo(new Verification(on("20 March"), on("20 June"), false, Optional.empty()));
        }

        @Test
        void aFailedRecheckCanBeRetriedBeforeTheDueDate() {
            aina.openRecheck(LIMITS, on("18 March"));

            EkycOutcome outcome = aina.recordEkycResult(EkycResult.rejected(), ACCEPTED, LIMITS, on("20 March"));

            assertThat(outcome).isEqualTo(EkycOutcome.RECHECK_FAILED);
            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.MEMBER);
            assertThat(aina.isTeamMember()).isTrue();
            assertThat(verification().recheckOpen()).isTrue();
            assertThat(verification().dueAt()).isEqualTo(on("1 April"));

            aina.recordEkycResult(verifiedAsAina(), ACCEPTED, LIMITS, on("25 March"));

            assertThat(verification().dueAt()).isEqualTo(on("25 June"));
        }

        @Test
        void aRepeatedVerifiedResultWithNoRecheckOpenChangesNothing() {
            EkycOutcome outcome = aina.recordEkycResult(verifiedAsAina(), ACCEPTED, LIMITS, on("2 January"));

            assertThat(outcome).isEqualTo(EkycOutcome.UNCHANGED);
            assertThat(verification()).isEqualTo(new Verification(on("1 January"), on("1 April"), false, Optional.empty()));
        }

        @Test
        void aRejectedResultWithNoRecheckOpenIsRefused() {
            assertThatThrownBy(() -> aina.recordEkycResult(EkycResult.rejected(), ACCEPTED, LIMITS, on("2 January")))
                    .isInstanceOf(TeamMemberAlreadyVerifiedException.class);
            assertThat(aina.isTeamMember()).isTrue();
        }
    }

    @Nested
    class Lapsing {

        @Test
        void missingTheDueDateLapsesTheMember() {
            aina.lapse(on("1 April"));

            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.LAPSED);
            assertThat(aina.isTeamMember()).isFalse();
        }

        @Test
        void aMemberDoesNotLapseBeforeTheDueDate() {
            assertThatThrownBy(() -> aina.lapse(on("31 March")))
                    .isInstanceOf(ReverificationNotYetDueException.class)
                    .hasMessageContaining(on("1 April").toString());
            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.MEMBER);
        }

        @Test
        void aMemberWithAnOpenRecheckStillLapsesAtTheDueDate() {
            aina.openRecheck(LIMITS, on("18 March"));

            aina.lapse(on("1 April"));

            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.LAPSED);
        }

        @Test
        void aLapsedMemberWhoPassesRejoinsTheTeam() {
            aina.lapse(on("1 April"));

            EkycOutcome outcome = aina.recordEkycResult(verifiedAsAina(), ACCEPTED, LIMITS, on("5 April"));

            assertThat(outcome).isEqualTo(EkycOutcome.REVERIFIED);
            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.MEMBER);
            assertThat(aina.isTeamMember()).isTrue();
            assertThat(verification()).isEqualTo(new Verification(on("5 April"), on("5 July"), false, Optional.empty()));
        }

        @Test
        void aLapsedMemberWhoFailsStaysLapsed() {
            aina.lapse(on("1 April"));

            EkycOutcome outcome = aina.recordEkycResult(EkycResult.rejected(), ACCEPTED, LIMITS, on("5 April"));

            assertThat(outcome).isEqualTo(EkycOutcome.RECHECK_FAILED);
            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.LAPSED);
        }
    }

    @Nested
    class EarlyRecheck {

        @Test
        void anEarlyRecheckMovesTheDueDateSooner() {
            aina.requestEarlyRecheck(" Report R12: different person attended ", LIMITS, on("10 January"));

            assertThat(verification().recheckOpen()).isTrue();
            assertThat(verification().dueAt()).isEqualTo(on("24 January"));
            assertThat(verification().earlyRecheckReason()).contains("Report R12: different person attended");
        }

        @Test
        void anEarlyRecheckNeverMovesTheDueDateLater() {
            aina.requestEarlyRecheck("Report R12", LIMITS, on("25 March"));

            assertThat(verification().recheckOpen()).isTrue();
            assertThat(verification().dueAt()).isEqualTo(on("1 April"));
        }

        @Test
        void anEarlyRecheckCanBeAskedForWhileTheRegularOneIsOpen() {
            aina.openRecheck(LIMITS, on("18 March"));

            aina.requestEarlyRecheck("Report R12", LIMITS, on("18 March"));

            assertThat(verification().dueAt()).isEqualTo(on("1 April"));
            assertThat(verification().earlyRecheckReason()).contains("Report R12");
        }

        @Test
        void passingAnEarlyRecheckStartsANewPeriod() {
            aina.requestEarlyRecheck("Report R12", LIMITS, on("10 January"));

            aina.recordEkycResult(verifiedAsAina(), ACCEPTED, LIMITS, on("12 January"));

            assertThat(verification()).isEqualTo(new Verification(on("12 January"), on("12 April"), false, Optional.empty()));
        }

        @Test
        void aMissedEarlyRecheckLapsesAtItsDeadline() {
            aina.requestEarlyRecheck("Report R12", LIMITS, on("10 January"));

            aina.lapse(on("24 January"));

            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.LAPSED);
        }

        @ParameterizedTest(name = "the reason \"{0}\" is not enough")
        @NullSource
        @ValueSource(strings = {"", "   "})
        void anEarlyRecheckNeedsAReason(String reason) {
            assertThatThrownBy(() -> aina.requestEarlyRecheck(reason, LIMITS, on("10 January")))
                    .isInstanceOf(RecheckReasonRequiredException.class);
            assertThat(verification().recheckOpen()).isFalse();
        }
    }

    @Nested
    class OnlyTeamMembers {

        static Stream<Named<Consumer<TeamMember>>> reverificationSteps() {
            return Stream.of(
                    Named.of("open a re-check", member -> member.openRecheck(LIMITS, on("18 March"))),
                    Named.of("ask for an early re-check", member -> member.requestEarlyRecheck("Report R12", LIMITS, on("18 March"))),
                    Named.of("lapse", member -> member.lapse(on("1 April"))));
        }

        @ParameterizedTest(name = "a lapsed member cannot {0}")
        @MethodSource("reverificationSteps")
        void reverificationStepsAreForMembersOnTheTeam(Consumer<TeamMember> step) {
            aina.lapse(on("1 April"));

            assertThatThrownBy(() -> step.accept(aina))
                    .isInstanceOf(NotATeamMemberException.class)
                    .hasMessageContaining(aina.id())
                    .hasMessageContaining("LAPSED");
            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.LAPSED);
        }

        @ParameterizedTest(name = "a removed member cannot {0}")
        @MethodSource("reverificationSteps")
        void aRemovedMemberIsNotReverified(Consumer<TeamMember> step) {
            aina.remove();

            assertThatThrownBy(() -> step.accept(aina)).isInstanceOf(TeamMemberRemovedException.class);
            assertThat(aina.teamStatus()).isEqualTo(TeamStatus.REMOVED);
        }

        @Test
        void aRemovedMemberTakesNoEkycResult() {
            aina.remove();

            assertThatThrownBy(() -> aina.recordEkycResult(verifiedAsAina(), ACCEPTED, LIMITS, on("2 January")))
                    .isInstanceOf(TeamMemberRemovedException.class)
                    .hasMessageContaining(aina.id());
        }
    }

    @Nested
    class Limits {

        @Test
        void theLimitsNeedAPeriodOfAtLeastAMonth() {
            assertThatThrownBy(() -> new VerificationLimits(0, Duration.ofDays(14), Duration.ofDays(14), Duration.ofHours(1)))
                    .isInstanceOf(InvalidVerificationLimitsException.class)
                    .hasMessageContaining("0");
            assertThat(new VerificationLimits(1, Duration.ofDays(14), Duration.ofDays(14), Duration.ofHours(1)).periodMonths()).isEqualTo(1);
        }

        static Stream<Duration> notLongerThanZero() {
            return Stream.of(Duration.ZERO, Duration.ofDays(-1), null);
        }

        @ParameterizedTest(name = "a reminder of {0} is refused")
        @MethodSource("notLongerThanZero")
        void theReminderIsLongerThanZero(Duration reminder) {
            assertThatThrownBy(() -> new VerificationLimits(3, reminder, Duration.ofDays(14), Duration.ofHours(1)))
                    .isInstanceOf(InvalidVerificationLimitsException.class);
        }

        @ParameterizedTest(name = "an early re-check deadline of {0} is refused")
        @MethodSource("notLongerThanZero")
        void theEarlyRecheckDeadlineIsLongerThanZero(Duration deadline) {
            assertThatThrownBy(() -> new VerificationLimits(3, Duration.ofDays(14), deadline, Duration.ofHours(1)))
                    .isInstanceOf(InvalidVerificationLimitsException.class);
        }

        @ParameterizedTest(name = "a verification session expiry of {0} is refused")
        @MethodSource("notLongerThanZero")
        void theVerificationSessionExpiryIsLongerThanZero(Duration expiry) {
            assertThatThrownBy(() -> new VerificationLimits(3, Duration.ofDays(14), Duration.ofDays(14), expiry))
                    .isInstanceOf(InvalidVerificationLimitsException.class);
        }

        @Test
        void theShortestLimitsAreAllowed() {
            VerificationLimits shortest =
                    new VerificationLimits(1, Duration.ofSeconds(1), Duration.ofSeconds(1), Duration.ofSeconds(1));

            assertThat(shortest.reminder()).isEqualTo(Duration.ofSeconds(1));
            assertThat(shortest.earlyRecheckDeadline()).isEqualTo(Duration.ofSeconds(1));
            assertThat(shortest.sessionExpiry()).isEqualTo(Duration.ofSeconds(1));
        }

        @Test
        void aVerificationAlwaysHasItsDates() {
            assertThatThrownBy(() -> new Verification(null, on("1 April"), false, Optional.empty()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Verification(on("1 January"), null, false, Optional.empty()))
                    .isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new Verification(on("1 January"), on("1 April"), false, null))
                    .isInstanceOf(NullPointerException.class);
        }
    }
}
