package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.support.ReservationFixtures.*;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import kernel.Money;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.CompletionAlreadySubmittedException;
import reservation.domain.exception.CustomerNoShowNotReportableException;
import reservation.domain.exception.JobTeamInvalidException;
import reservation.domain.exception.JobTeamNotNamedException;
import reservation.domain.exception.NoShowReportWindowClosedException;
import reservation.domain.exception.NotCheckedInException;
import reservation.domain.exception.OutsideCheckInRadiusException;
import reservation.domain.exception.OutsideCheckInWindowException;
import reservation.domain.exception.PersonCannotWorkException;
import reservation.domain.exception.ReservationStatusException;

class ReservationJobDayTest {

    private static final Instant EARLIEST = START.minus(Duration.ofMinutes(15));
    private static final Instant CLOSES = START.plus(Duration.ofMinutes(15));

    private static Reservation threeHelpers() {
        Reservation reservation = Reservation.request(customer(), terms(), VERIFIED, freeSchedule(),
                new BookingDetails(Set.of(KITCHEN), START, 3, 3), VALUES, NOW);
        reservation.securePayment();
        reservation.accept(VERIFIED, freeSchedule(), NOW);
        return reservation;
    }

    @Test
    void theProviderNamesTheHelpersMatchingTheNumberBooked() {
        Reservation reservation = reserved();
        assertThat(reservation.isTeamNamed()).isFalse();

        reservation.nameJobTeam(Set.of("helper-2", "helper-1"), ROSTER, NOW);

        assertThat(reservation.isTeamNamed()).isTrue();
        assertThat(reservation.jobTeam().orElseThrow().people()).containsExactly(
                new NamedPerson("helper-1", TeamRole.HELPER), new NamedPerson("helper-2", TeamRole.HELPER));
        assertThat(reservation.jobTeam().orElseThrow().memberIds()).containsExactlyInAnyOrder("helper-1", "helper-2");
    }

    @Test
    void theHelpersNamedMustMatchTheHelpersBooked() {
        Reservation reservation = reserved();

        assertThatThrownBy(() -> reservation.nameJobTeam(Set.of("helper-1"), ROSTER, NOW))
                .isInstanceOf(JobTeamInvalidException.class);
        assertThatThrownBy(() -> reservation.nameJobTeam(Set.of("helper-1", "helper-2", "helper-3"), ROSTER, NOW))
                .isInstanceOf(JobTeamInvalidException.class);
        assertThat(reservation.isTeamNamed()).isFalse();
    }

    @Test
    void aSupervisorCanAlwaysComeAndIsNotCountedAsAHelper() {
        Reservation reservation = reserved();

        reservation.nameJobTeam(Set.of("helper-1", "helper-2", "supervisor-1"), ROSTER, NOW);

        assertThat(reservation.isTeamNamed()).isTrue();
        assertThat(reservation.jobTeam().orElseThrow().people()).hasSize(3);
    }

    @Test
    void aBookingThatIncludesASupervisorNeedsOneNamed() {
        Reservation reservation = threeHelpers();

        assertThatThrownBy(() -> reservation.nameJobTeam(Set.of("helper-1", "helper-2", "helper-3"), ROSTER, NOW))
                .isInstanceOf(JobTeamInvalidException.class);

        reservation.nameJobTeam(Set.of("helper-1", "helper-2", "helper-3", "supervisor-1"), ROSTER, NOW);
        assertThat(reservation.isTeamNamed()).isTrue();
    }

    @Test
    void onlyPeopleOnTheTeamCanBeNamed() {
        assertThatThrownBy(() -> reserved().nameJobTeam(Set.of("helper-1", "stranger"), ROSTER, NOW))
                .isInstanceOf(JobTeamInvalidException.class);
        assertThat(ROSTER.without("helper-1").roleOf("helper-1")).isEmpty();
        assertThat(TeamRoster.empty(PROVIDER).with("x", TeamRole.SUPERVISOR).roleOf("x")).contains(TeamRole.SUPERVISOR);
    }

    @Test
    void theTeamCanBeChangedUntilTheBookedStart() {
        Reservation reservation = teamNamed();

        reservation.nameJobTeam(Set.of("helper-2", "helper-3"), ROSTER, START.minusSeconds(1));
        assertThat(reservation.jobTeam().orElseThrow().memberIds()).containsExactlyInAnyOrder("helper-2", "helper-3");

        assertThatThrownBy(() -> reservation.nameJobTeam(Set.of("helper-1", "helper-2"), ROSTER, START))
                .isInstanceOf(JobTeamInvalidException.class);
        assertThatThrownBy(() -> secured(terms()).nameJobTeam(Set.of("helper-1", "helper-2"), ROSTER, NOW))
                .isInstanceOf(ReservationStatusException.class);
    }

    @Test
    void aNamedPersonWhoLapsesOrIsRemovedComesOffTheTeamAndItCountsAsNotNamed() {
        Reservation reservation = teamNamed();
        reservation.remindTeamNaming(START.minus(Duration.ofHours(6)));

        assertThat(reservation.removeFromJobTeam("helper-2")).isTrue();

        assertThat(reservation.isTeamNamed()).isFalse();
        assertThat(reservation.jobTeam().orElseThrow().memberIds()).containsExactly("helper-1");
        assertThat(reservation.removeFromJobTeam("helper-2")).isFalse();
        assertThat(reservation.removeFromJobTeam("helper-3")).isFalse();
        assertThat(reserved().removeFromJobTeam("helper-1")).isFalse();
        assertThat(reservation.remindTeamNaming(START.minus(Duration.ofHours(5)))).isTrue();
    }

    @Test
    void aPersonIsNotRemovedFromAJobThatHasEnded() {
        Reservation reservation = teamNamed();
        reservation.cancelByProvider(NOW);

        assertThat(reservation.removeFromJobTeam("helper-1")).isFalse();
        assertThat(reservation.jobTeam().orElseThrow().memberIds()).hasSize(2);
    }

    @Test
    void homaRemindsTheProviderOnceAtTheTeamReminderTimeWhileTheTeamIsNotNamed() {
        Reservation reservation = reserved();
        Instant reminderTime = START.minus(Duration.ofHours(6));

        assertThat(reservation.remindTeamNaming(reminderTime.minusSeconds(1))).isFalse();
        assertThat(reservation.remindTeamNaming(reminderTime)).isTrue();
        assertThat(reservation.remindTeamNaming(reminderTime.plusSeconds(60))).isFalse();
        assertThat(teamNamed().remindTeamNaming(reminderTime)).isFalse();
        assertThat(secured(terms()).remindTeamNaming(reminderTime)).isFalse();
    }

    @Test
    void checkInIsEnabledOnlyOnceTheTeamIsNamed() {
        Reservation reservation = reserved();

        assertThatThrownBy(() -> reservation.checkIn("helper-1", FaceCheckResult.PASSED, true, START))
                .isInstanceOf(JobTeamNotNamedException.class);
        assertThat(reservation.isCheckedIn()).isFalse();
    }

    @Test
    void theProviderChecksInFromTheEarliestCheckInUpToAndIncludingTheEndOfTheCheckInPeriod() {
        Reservation early = teamNamed();
        Reservation late = teamNamed();
        assertThat(late.checkInClosesAt()).isEqualTo(CLOSES);

        early.checkIn("helper-1", FaceCheckResult.PASSED, true, EARLIEST);
        late.checkIn("helper-1", FaceCheckResult.PASSED, true, CLOSES);

        assertThat(early.isCheckedIn()).isTrue();
        assertThat(early.state().checkedInAt()).isEqualTo(EARLIEST);
        assertThat(late.state().checkedInAt()).isEqualTo(CLOSES);
        assertThatThrownBy(() -> teamNamed().checkIn("helper-1", FaceCheckResult.PASSED, true, EARLIEST.minusSeconds(1)))
                .isInstanceOf(OutsideCheckInWindowException.class);
        assertThatThrownBy(() -> teamNamed().checkIn("helper-1", FaceCheckResult.PASSED, true, CLOSES.plusSeconds(1)))
                .isInstanceOf(OutsideCheckInWindowException.class);
    }

    @Test
    void checkInNeedsTheDeviceWithinTheCheckInRadius() {
        Reservation reservation = teamNamed();

        assertThatThrownBy(() -> reservation.checkIn("helper-1", FaceCheckResult.PASSED, false, START))
                .isInstanceOf(OutsideCheckInRadiusException.class);
        assertThat(reservation.isCheckedIn()).isFalse();
        assertThat(reservation.state().faceChecks()).isEmpty();
    }

    @Test
    void checkInCountsOnceTheFirstNamedPersonPassesTheFaceCheck() {
        Reservation reservation = teamNamed();

        reservation.checkIn("helper-1", FaceCheckResult.FAILED, true, START);
        assertThat(reservation.isCheckedIn()).isFalse();
        assertThat(reservation.canWork("helper-1")).isFalse();

        reservation.checkIn("helper-2", FaceCheckResult.PASSED, true, START.plusSeconds(60));
        assertThat(reservation.isCheckedIn()).isTrue();
        assertThat(reservation.state().checkedInAt()).isEqualTo(START.plusSeconds(60));
        assertThat(reservation.state().faceChecks().get("helper-1"))
                .isEqualTo(new FaceCheck("helper-1", FaceCheckResult.FAILED, START));
        assertThat(reservation.canWork("helper-2")).isTrue();
    }

    @Test
    void whenTheFaceCheckServiceIsUnavailableCheckInGoesAheadOnLocationOnly() {
        Reservation reservation = teamNamed();

        reservation.checkIn("helper-1", FaceCheckResult.UNAVAILABLE, true, START);

        assertThat(reservation.isCheckedIn()).isTrue();
        assertThat(reservation.state().faceChecks().get("helper-1").result()).isEqualTo(FaceCheckResult.UNAVAILABLE);
        assertThat(reservation.canWork("helper-1")).isTrue();
    }

    @Test
    void eachOtherNamedPersonPassesTheFaceCheckBeforeWorking() {
        Reservation reservation = checkedIn();
        assertThat(reservation.canWork("helper-2")).isTrue();

        reservation.faceCheck("helper-2", FaceCheckResult.FAILED, START.plusSeconds(120));

        assertThat(reservation.canWork("helper-2")).isFalse();
        assertThat(reservation.canWork("helper-1")).isTrue();
        assertThat(reservation.canWork("helper-3")).isFalse();
        assertThat(reserved().canWork("helper-1")).isFalse();
        assertThat(reservation.state().faceChecks().get("helper-2").checkedAt()).isEqualTo(START.plusSeconds(120));
    }

    @Test
    void onlyNamedPeopleTakeTheFaceCheckAndOnlyAfterCheckInForTheOthers() {
        assertThatThrownBy(() -> teamNamed().checkIn("helper-3", FaceCheckResult.PASSED, true, START))
                .isInstanceOf(PersonCannotWorkException.class);
        assertThatThrownBy(() -> checkedIn().faceCheck("helper-3", FaceCheckResult.PASSED, START))
                .isInstanceOf(PersonCannotWorkException.class);
        assertThatThrownBy(() -> teamNamed().faceCheck("helper-2", FaceCheckResult.PASSED, START))
                .isInstanceOf(NotCheckedInException.class);
        assertThatThrownBy(() -> checkedIn().checkIn("helper-2", FaceCheckResult.PASSED, true, START))
                .isInstanceOf(ReservationStatusException.class);
    }

    @Test
    void noCheckInByTheEndOfTheCheckInPeriodIsAProviderNoShow() {
        Reservation reservation = teamNamed();

        assertThat(reservation.endAsProviderNoShow(CLOSES)).isFalse();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);

        assertThat(reservation.endAsProviderNoShow(CLOSES.plusSeconds(1))).isTrue();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.ending()).contains(Ending.PROVIDER_NO_SHOW);
        assertThat(reservation.settlement()).contains(Settlement.fullRefund());
        assertThat(reservation.fine()).contains(new Fine(Money.ofRinggit(54), FineReason.NO_SHOW));
        assertThat(reservation.endAsProviderNoShow(CLOSES.plusSeconds(2))).isFalse();
    }

    @Test
    void aProviderWhoCheckedInIsNotANoShow() {
        Reservation reservation = checkedIn();

        assertThat(reservation.endAsProviderNoShow(CLOSES.plusSeconds(1))).isFalse();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.RESERVED);
        assertThat(reservation.fine()).isEmpty();
    }

    @Test
    void theProviderReportsACustomerNoShowAfterTheWaitAndIsDueTheNoShowFee() {
        Reservation reservation = checkedIn();
        Instant afterTheWait = START.plus(Duration.ofMinutes(15));

        assertThatThrownBy(() -> reservation.reportCustomerNoShow(afterTheWait.minusSeconds(1)))
                .isInstanceOf(CustomerNoShowNotReportableException.class);
        reservation.reportCustomerNoShow(afterTheWait);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.ending()).contains(Ending.CUSTOMER_NO_SHOW);
        assertThat(reservation.settlement()).contains(new Settlement(Money.ofRinggit(64), PayoutKind.CUSTOMER_NO_SHOW_FEE));
        assertThat(reservation.noShowReportWindowClosesAt()).contains(afterTheWait.plus(Duration.ofHours(1)));
    }

    @Test
    void theWaitRunsFromTheCheckInWhenTheProviderChecksInAfterTheStart() {
        Reservation reservation = teamNamed();
        Instant lateCheckIn = START.plus(Duration.ofMinutes(10));
        reservation.checkIn("helper-1", FaceCheckResult.PASSED, true, lateCheckIn);

        assertThatThrownBy(() -> reservation.reportCustomerNoShow(START.plus(Duration.ofMinutes(20))))
                .isInstanceOf(CustomerNoShowNotReportableException.class);
        reservation.reportCustomerNoShow(lateCheckIn.plus(Duration.ofMinutes(15)));

        assertThat(reservation.ending()).contains(Ending.CUSTOMER_NO_SHOW);
    }

    @Test
    void theWaitRunsFromTheBookedStartWhenTheProviderChecksInEarly() {
        Reservation reservation = teamNamed();
        reservation.checkIn("helper-1", FaceCheckResult.PASSED, true, EARLIEST);

        assertThatThrownBy(() -> reservation.reportCustomerNoShow(START.plus(Duration.ofMinutes(14))))
                .isInstanceOf(CustomerNoShowNotReportableException.class);
        reservation.reportCustomerNoShow(START.plus(Duration.ofMinutes(15)));

        assertThat(reservation.ending()).contains(Ending.CUSTOMER_NO_SHOW);
    }

    @Test
    void aCustomerNoShowIsReportedUpToAndIncludingTheBookedEnd() {
        Reservation atTheEnd = checkedIn();
        atTheEnd.reportCustomerNoShow(END);
        assertThat(atTheEnd.ending()).contains(Ending.CUSTOMER_NO_SHOW);

        assertThatThrownBy(() -> checkedIn().reportCustomerNoShow(END.plusSeconds(1)))
                .isInstanceOf(CustomerNoShowNotReportableException.class);
    }

    @Test
    void aCustomerNoShowNeedsACheckInAndCannotFollowCompletion() {
        assertThatThrownBy(() -> teamNamed().reportCustomerNoShow(START.plus(Duration.ofMinutes(20))))
                .isInstanceOf(NotCheckedInException.class);
        assertThatThrownBy(() -> completionSubmitted().reportCustomerNoShow(END))
                .isInstanceOf(CompletionAlreadySubmittedException.class);
        assertThat(checkedIn().noShowReportWindowClosesAt()).isEmpty();
    }

    @Test
    void theNoShowFeeIsReleasedWhenTheReportWindowEndsWithoutAChallenge() {
        Reservation reservation = checkedIn();
        Instant reported = START.plus(Duration.ofMinutes(20));
        reservation.reportCustomerNoShow(reported);
        Instant closes = reported.plus(Duration.ofHours(1));

        assertThat(reservation.releaseCustomerNoShowFee(closes.minusSeconds(1))).isFalse();
        assertThat(reservation.releaseCustomerNoShowFee(closes)).isTrue();

        assertThat(reservation.state().customerNoShowFeeReleased()).isTrue();
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(reservation.settlement()).contains(new Settlement(Money.ofRinggit(64), PayoutKind.CUSTOMER_NO_SHOW_FEE));
        assertThat(reservation.releaseCustomerNoShowFee(closes.plusSeconds(1))).isFalse();
        assertThat(checkedIn().releaseCustomerNoShowFee(closes)).isFalse();
    }

    @Test
    void theCustomerChallengesANoShowWithinTheReportWindowAndHomaInvestigates() {
        Reservation reservation = checkedIn();
        Instant reported = START.plus(Duration.ofMinutes(20));
        reservation.reportCustomerNoShow(reported);

        reservation.challengeNoShow(reported.plus(Duration.ofHours(1)).minusSeconds(1));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.DISPUTED);
        assertThat(reservation.disputeCause()).contains(DisputeCause.NO_SHOW_CHALLENGE);
        assertThat(reservation.settlement()).isEmpty();
        assertThat(reservation.atStake()).isEqualTo(Money.ofRinggit(64));
        assertThat(reservation.releaseCustomerNoShowFee(reported.plus(Duration.ofHours(2)))).isFalse();
    }

    @Test
    void theNoShowReportWindowClosesAtTheMomentTheFeeIsReleased() {
        Reservation reservation = checkedIn();
        Instant reported = START.plus(Duration.ofMinutes(20));
        reservation.reportCustomerNoShow(reported);

        assertThatThrownBy(() -> reservation.challengeNoShow(reported.plus(Duration.ofHours(1))))
                .isInstanceOf(NoShowReportWindowClosedException.class);
        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);

        reservation.releaseCustomerNoShowFee(reported.plus(Duration.ofHours(1)));
        assertThatThrownBy(() -> reservation.challengeNoShow(reported.plusSeconds(60)))
                .isInstanceOf(ReservationStatusException.class);
        assertThatThrownBy(() -> checkedIn().challengeNoShow(START)).isInstanceOf(ReservationStatusException.class);
    }

    @Test
    void aCustomerCancellationAfterCheckInCannotBeChallenged() {
        Reservation reservation = checkedIn();
        reservation.cancelByCustomer(START.plusSeconds(60));

        assertThatThrownBy(() -> reservation.challengeNoShow(START.plusSeconds(120)))
                .isInstanceOf(ReservationStatusException.class);
    }
}
