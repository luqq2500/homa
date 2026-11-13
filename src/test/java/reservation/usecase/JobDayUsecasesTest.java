package reservation.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.support.ReservationFixtures.*;
import static reservation.support.ReservationWorld.AT_THE_DOOR;

import ie.events.CompletionReminderIE;
import ie.events.CustomerNoShowReportedIE;
import ie.events.FaceCheckFailedIE;
import ie.events.JobTeamMemberRemovedIE;
import ie.events.JobTeamNamedIE;
import ie.events.NoShowChallengedIE;
import ie.events.ProviderCheckedInIE;
import ie.events.ProviderFinedIE;
import ie.events.ReservationEndedIE;
import ie.events.ReservationSettledIE;
import ie.events.TeamMemberLeftIE;
import ie.events.TeamNamingReminderIE;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import kernel.Money;
import org.junit.jupiter.api.Test;
import reservation.domain.FaceCheckResult;
import reservation.domain.ReservationStatus;
import reservation.domain.exception.OutsideCheckInRadiusException;
import reservation.support.ReservationWorld;
import reservation.usecase.dto.ChallengeNoShowRequest;
import reservation.usecase.dto.CheckInRequest;
import reservation.usecase.dto.CheckInResponse;
import reservation.usecase.dto.FaceCheckRequest;
import reservation.usecase.dto.FaceCheckResponse;
import reservation.usecase.dto.NameJobTeamRequest;
import reservation.usecase.dto.NameJobTeamResponse;
import reservation.usecase.dto.ReportCustomerNoShowRequest;
import reservation.usecase.dto.ReportCustomerNoShowResponse;

class JobDayUsecasesTest {

    private final ReservationWorld world = new ReservationWorld();
    private final ProviderCheckIn checkIn =
            new ProviderCheckIn(world.reservations, world.locations, world.faces, world.publisher, world.clock);

    @Test
    void theNamedTeamIsSavedAndTheCustomerCanSeeWhoIsComing() {
        String id = world.reserved();

        NameJobTeamResponse response = new ProviderNameJobTeam(world.reservations, world.rosters, world.publisher,
                world.clock).name(new NameJobTeamRequest(PROVIDER, id, Set.of("helper-1", "helper-3")));

        assertThat(response).isEqualTo(new NameJobTeamResponse(id, Set.of("helper-1", "helper-3")));
        assertThat(world.reservations.timesSaved(id)).isEqualTo(4);
        assertThat(world.publisher.published(JobTeamNamedIE.class)).containsExactly(
                new JobTeamNamedIE(id, CUSTOMER, PROVIDER, Set.of("helper-1", "helper-3"), NOW));
    }

    @Test
    void aTeamMemberWhoLeavesComesOffEveryUpcomingJobTheyWereNamedOn() {
        String id = world.teamNamed();
        String other = world.reserved(START.plus(Duration.ofDays(1)));
        HandleTeamMemberLeftIE handler = new HandleTeamMemberLeftIE(world.rosters, world.reservations,
                world.publisher, world.clock);

        handler.handle(new TeamMemberLeftIE("helper-2", PROVIDER, "LAPSED", NOW));

        assertThat(world.rosters.find(PROVIDER).orElseThrow().roleOf("helper-2")).isEmpty();
        assertThat(world.reservation(id).isTeamNamed()).isFalse();
        assertThat(world.reservations.timesSaved(id)).isEqualTo(5);
        assertThat(world.reservations.timesSaved(other)).isEqualTo(3);
        assertThat(world.publisher.published(JobTeamMemberRemovedIE.class))
                .containsExactly(new JobTeamMemberRemovedIE(id, CUSTOMER, PROVIDER, "helper-2", NOW));
    }

    @Test
    void theProviderIsRemindedOnceToNameTheTeam() {
        String id = world.reserved(START.plus(Duration.ofHours(4)));
        world.teamNamed();
        SystemRemindTeamNaming usecase = new SystemRemindTeamNaming(world.reservations, world.publisher, world.clock);
        world.clock.set(START.minus(Duration.ofHours(2)));

        assertThat(usecase.remindDue().ids()).containsExactly(id);
        assertThat(usecase.remindDue().ids()).isEmpty();

        assertThat(world.reservations.timesSaved(id)).isEqualTo(4);
        assertThat(world.publisher.published(TeamNamingReminderIE.class))
                .containsExactly(new TeamNamingReminderIE(id, PROVIDER, world.clock.instant()));
    }

    @Test
    void checkInUsesTheCheckInRadiusTheReservationWasRequestedWith() {
        String id = world.teamNamed();
        world.clock.set(START);

        CheckInResponse response = checkIn.checkIn(new CheckInRequest(PROVIDER, id, "helper-1", "selfie", AT_THE_DOOR));

        assertThat(response).isEqualTo(new CheckInResponse(id, true, "PASSED"));
        assertThat(world.radiusAsked).isEqualTo(300);
        assertThat(world.reservation(id).isCheckedIn()).isTrue();
        assertThat(world.reservations.timesSaved(id)).isEqualTo(5);
        assertThat(world.publisher.published(ProviderCheckedInIE.class))
                .containsExactly(new ProviderCheckedInIE(id, CUSTOMER, PROVIDER, START, false, START));
        assertThat(world.publisher.published(FaceCheckFailedIE.class)).isEmpty();
    }

    @Test
    void outsideTheRadiusThereIsNoCheckIn() {
        String id = world.teamNamed();
        world.clock.set(START);
        world.withinRadius = false;

        assertThatThrownBy(() -> checkIn.checkIn(new CheckInRequest(PROVIDER, id, "helper-1", "selfie", AT_THE_DOOR)))
                .isInstanceOf(OutsideCheckInRadiusException.class);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(4);
        assertThat(world.publisher.published(ProviderCheckedInIE.class)).isEmpty();
    }

    @Test
    void aFailedFaceCheckIsRecordedAndTheCustomerIsToldButItIsNotACheckIn() {
        String id = world.teamNamed();
        world.clock.set(START);
        world.faceAnswers.put("helper-1", FaceCheckResult.FAILED);

        CheckInResponse response = checkIn.checkIn(new CheckInRequest(PROVIDER, id, "helper-1", "selfie", AT_THE_DOOR));

        assertThat(response).isEqualTo(new CheckInResponse(id, false, "FAILED"));
        assertThat(world.reservations.timesSaved(id)).isEqualTo(5);
        assertThat(world.publisher.published(ProviderCheckedInIE.class)).isEmpty();
        assertThat(world.publisher.published(FaceCheckFailedIE.class))
                .containsExactly(new FaceCheckFailedIE(id, CUSTOMER, PROVIDER, "helper-1", START));
    }

    @Test
    void checkInGoesAheadMarkedFaceCheckUnavailableWhenTheServiceIsDown() {
        String id = world.teamNamed();
        world.clock.set(START);
        world.faceAnswers.put("helper-1", FaceCheckResult.UNAVAILABLE);

        checkIn.checkIn(new CheckInRequest(PROVIDER, id, "helper-1", "selfie", AT_THE_DOOR));

        assertThat(world.publisher.published(ProviderCheckedInIE.class))
                .containsExactly(new ProviderCheckedInIE(id, CUSTOMER, PROVIDER, START, true, START));
    }

    @Test
    void eachOtherPersonTakesTheFaceCheckAfterCheckIn() {
        String id = world.checkedIn();
        ProviderFaceCheck usecase = new ProviderFaceCheck(world.reservations, world.faces, world.publisher, world.clock);
        world.faceAnswers.put("helper-2", FaceCheckResult.FAILED);

        FaceCheckResponse response = usecase.check(new FaceCheckRequest(PROVIDER, id, "helper-2", "selfie"));

        assertThat(response).isEqualTo(new FaceCheckResponse(id, "FAILED", false));
        assertThat(world.reservations.timesSaved(id)).isEqualTo(6);
        assertThat(world.publisher.published(FaceCheckFailedIE.class))
                .containsExactly(new FaceCheckFailedIE(id, CUSTOMER, PROVIDER, "helper-2", START));

        world.faceAnswers.clear();
        assertThat(usecase.check(new FaceCheckRequest(PROVIDER, id, "helper-2", "selfie")))
                .isEqualTo(new FaceCheckResponse(id, "PASSED", true));
        assertThat(world.publisher.published(FaceCheckFailedIE.class)).hasSize(1);
    }

    @Test
    void aProviderWhoDoesNotCheckInIsANoShowRefundedAndFined() {
        String id = world.teamNamed();
        SystemEndProviderNoShows usecase = new SystemEndProviderNoShows(world.reservations, world.publisher, world.clock);
        Instant after = START.plus(Duration.ofMinutes(15)).plusSeconds(1);
        world.clock.set(after);

        assertThat(usecase.endDue().ids()).containsExactly(id);

        assertThat(world.reservation(id).status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(world.reservations.timesSaved(id)).isEqualTo(5);
        assertThat(world.publisher.published(ReservationEndedIE.class)).containsExactly(new ReservationEndedIE(id,
                CUSTOMER, PROVIDER, "CANCELLED", "PROVIDER_NO_SHOW", true, START, END, after));
        assertThat(usecase.endDue().ids()).isEmpty();
        assertThat(world.publisher.published(ReservationSettledIE.class))
                .containsExactly(new ReservationSettledIE(id, PROVIDER, Money.ZERO, "JOB", after));
        assertThat(world.publisher.published(ProviderFinedIE.class))
                .containsExactly(new ProviderFinedIE(id, PROVIDER, Money.ofRinggit(54), "NO_SHOW", after));
    }

    @Test
    void aReportedCustomerNoShowHoldsTheFeeUntilTheReportWindowEnds() {
        String id = world.checkedIn();
        Instant reported = START.plus(Duration.ofMinutes(20));
        world.clock.set(reported);

        ReportCustomerNoShowResponse response = new ProviderReportCustomerNoShow(world.reservations, world.publisher,
                world.clock).report(new ReportCustomerNoShowRequest(PROVIDER, id));

        assertThat(response).isEqualTo(
                new ReportCustomerNoShowResponse(id, Money.ofRinggit(64), reported.plus(Duration.ofHours(1))));
        assertThat(world.reservations.timesSaved(id)).isEqualTo(6);
        assertThat(world.publisher.published(ReservationEndedIE.class)).containsExactly(new ReservationEndedIE(id,
                CUSTOMER, PROVIDER, "CANCELLED", "CUSTOMER_NO_SHOW", true, START, END, reported));
        assertThat(world.publisher.published(ReservationSettledIE.class)).isEmpty();
        assertThat(world.publisher.published(CustomerNoShowReportedIE.class)).containsExactly(
                new CustomerNoShowReportedIE(id, CUSTOMER, PROVIDER, reported.plus(Duration.ofHours(1)), reported));

        SystemReleaseCustomerNoShowFees releaser =
                new SystemReleaseCustomerNoShowFees(world.reservations, world.publisher, world.clock);
        assertThat(releaser.releaseDue().ids()).isEmpty();
        world.clock.set(reported.plus(Duration.ofHours(1)));
        assertThat(releaser.releaseDue().ids()).containsExactly(id);
        assertThat(releaser.releaseDue().ids()).isEmpty();
        assertThat(world.reservations.timesSaved(id)).isEqualTo(7);
        assertThat(world.publisher.published(ReservationSettledIE.class)).containsExactly(new ReservationSettledIE(id,
                PROVIDER, Money.ofRinggit(64), "CUSTOMER_NO_SHOW_FEE", reported.plus(Duration.ofHours(1))));
    }

    @Test
    void aChallengedNoShowGoesToHomaAndTheFeeIsNotReleased() {
        String id = world.checkedIn();
        world.clock.set(START.plus(Duration.ofMinutes(20)));
        new ProviderReportCustomerNoShow(world.reservations, world.publisher, world.clock)
                .report(new ReportCustomerNoShowRequest(PROVIDER, id));
        world.clock.set(START.plus(Duration.ofMinutes(30)));

        String status = new CustomerChallengeNoShow(world.reservations, world.publisher, world.clock)
                .challenge(new ChallengeNoShowRequest(CUSTOMER, id)).status();

        assertThat(status).isEqualTo("DISPUTED");
        assertThat(world.reservations.timesSaved(id)).isEqualTo(7);
        assertThat(world.publisher.published(NoShowChallengedIE.class)).containsExactly(
                new NoShowChallengedIE(id, CUSTOMER, PROVIDER, Money.ofRinggit(64), world.clock.instant()));
        world.clock.set(START.plus(Duration.ofHours(3)));
        assertThat(new SystemReleaseCustomerNoShowFees(world.reservations, world.publisher, world.clock)
                .releaseDue().ids()).isEmpty();
    }

    @Test
    void theProviderIsRemindedOnceToSubmitCompletion() {
        String id = world.checkedIn();
        SystemRemindCompletion usecase = new SystemRemindCompletion(world.reservations, world.publisher, world.clock);
        world.clock.set(END.plus(Duration.ofMinutes(30)));

        assertThat(usecase.remindDue().ids()).containsExactly(id);
        assertThat(usecase.remindDue().ids()).isEmpty();

        assertThat(world.reservations.timesSaved(id)).isEqualTo(6);
        assertThat(world.publisher.published(CompletionReminderIE.class))
                .containsExactly(new CompletionReminderIE(id, PROVIDER, world.clock.instant()));
    }
}
