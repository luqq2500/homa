package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.A_PHOTO;
import static reservation.domain.ReservationBuilders.ROSTER;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.anOffering;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.rm;
import static reservation.domain.ReservationBuilders.saturday;

import java.util.List;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.TeamNotNamedException;

/**
 * Feature: A person who lapses or is removed comes off the team. Background: reserved for Saturday 10:00 to
 * 13:00 for 2 helpers, with Aina and Ben named.
 */
class ReservationTeamDropTest {

    @Test
    void beforeTheStartTheTeamIsIncompleteUntilNamedAgain() {
        Reservation reservation = aReservation().reservedWithTeamNamed();

        TeamDrop drop = reservation.dropFromTeam("ben", friday("18:00"));

        assertThat(drop).isEqualTo(TeamDrop.TEAM_INCOMPLETE);
        assertThat(reservation.isTeamComplete()).isFalse();
        assertThat(reservation.team().orElseThrow().helpers()).containsExactly("aina");
        assertThat(reservation.isTeamReminderDue(saturday("03:59"))).isFalse();
        assertThat(reservation.isTeamReminderDue(saturday("04:00"))).isTrue();
        assertThatThrownBy(() -> reservation.checkIn(120, saturday("09:50"))).isInstanceOf(TeamNotNamedException.class);
    }

    @Test
    void justBeforeTheStartThePersonStillComesOffTheTeam() {
        Reservation reservation = aReservation().reservedWithTeamNamed();

        assertThat(reservation.dropFromTeam("ben", saturday("09:59"))).isEqualTo(TeamDrop.TEAM_INCOMPLETE);
    }

    @Test
    void namingSomeoneInTheirPlaceCompletesTheTeam() {
        Reservation reservation = aReservation().reservedWithTeamNamed();
        reservation.dropFromTeam("ben", friday("18:00"));

        reservation.nameTeam(TeamAssignment.helpers("aina", "chan"), ROSTER, friday("19:00"));

        assertThat(reservation.isTeamComplete()).isTrue();
        assertThat(reservation.isTeamReminderDue(saturday("04:00"))).isFalse();
        reservation.checkIn(120, saturday("09:50"));
        assertThat(reservation.checkInRecord()).isPresent();
    }

    @Test
    void aTeamStillIncompleteAtTheEndOfTheCheckInPeriodIsAProviderNoShow() {
        Reservation reservation = aReservation().reservedWithTeamNamed();
        reservation.dropFromTeam("ben", friday("18:00"));

        CancellationResult result = reservation.markProviderNoShow(saturday("10:16"));

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(result.fine().orElseThrow().amount()).isEqualTo(rm("54.00"));
    }

    @Test
    void duringTheJobTheyStopWorking() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
        reservation.recordFaceCheck("ben", FaceCheckResult.PASSED, saturday("10:01"));

        TeamDrop drop = reservation.dropFromTeam("ben", saturday("11:00"));

        assertThat(drop).isEqualTo(TeamDrop.CANNOT_WORK);
        assertThat(reservation.canWork("ben")).isFalse();
        assertThat(reservation.canWork("aina")).isTrue();
        assertThat(reservation.isCheckedIn()).isTrue();
        assertThat(reservation.team().orElseThrow().helpers()).containsExactlyInAnyOrder("aina", "ben");
    }

    @Test
    void fromTheBookedStartTheTeamIsLockedSoThePersonCannotWork() {
        Reservation reservation = aReservation().reservedWithTeamNamed();

        assertThat(reservation.dropFromTeam("ben", saturday("10:00"))).isEqualTo(TeamDrop.CANNOT_WORK);
        assertThat(reservation.isTeamComplete()).isTrue();
    }

    @Test
    void aJobUnderDisputeStillStopsThePerson() {
        Reservation reservation = aReservation().checkedInAt(saturday("10:00"));
        reservation.fileReport(Party.CUSTOMER, ReportType.DURING_JOB, "Only one helper came", List.of(A_PHOTO), saturday("10:30"));

        assertThat(reservation.dropFromTeam("aina", saturday("11:00"))).isEqualTo(TeamDrop.CANNOT_WORK);
        assertThat(reservation.canWork("aina")).isFalse();
    }

    @Test
    void someoneNotNamedChangesNothing() {
        Reservation reservation = aReservation().reservedWithTeamNamed();

        assertThat(reservation.dropFromTeam("dina", friday("18:00"))).isEqualTo(TeamDrop.NOT_AFFECTED);
        assertThat(reservation.isTeamComplete()).isTrue();
    }

    @Test
    void aReservationThatIsNotUpcomingIsNotAffected() {
        Reservation cancelled = aReservation().reservedWithTeamNamed();
        cancelled.cancelByCustomer(friday("09:00"));
        Reservation request = aReservation().requested();

        assertThat(cancelled.dropFromTeam("ben", friday("18:00"))).isEqualTo(TeamDrop.NOT_AFFECTED);
        assertThat(cancelled.team().orElseThrow().helpers()).containsExactlyInAnyOrder("aina", "ben");
        assertThat(request.dropFromTeam("ben", friday("18:00"))).isEqualTo(TeamDrop.NOT_AFFECTED);
    }

    @Test
    void anOptionalSupervisorComesOffAndTheTeamStaysComplete() {
        Reservation reservation = aReservation().reserved();
        reservation.nameTeam(TeamAssignment.helpers("aina", "ben").withSupervisors("siti"), ROSTER, friday("12:00"));

        TeamDrop drop = reservation.dropFromTeam("siti", friday("18:00"));

        assertThat(drop).isEqualTo(TeamDrop.REMOVED);
        assertThat(reservation.isTeamComplete()).isTrue();
        assertThat(reservation.team().orElseThrow().supervisors()).isEmpty();
    }

    @Test
    void aRequiredSupervisorComingOffLeavesTheTeamIncomplete() {
        Reservation reservation = aReservation().withOffering(anOffering().sendingASupervisorFrom(2, 3)).reserved();
        reservation.nameTeam(TeamAssignment.helpers("aina", "ben").withSupervisors("siti"), ROSTER, friday("12:00"));

        TeamDrop drop = reservation.dropFromTeam("siti", friday("18:00"));

        assertThat(drop).isEqualTo(TeamDrop.TEAM_INCOMPLETE);
        assertThat(reservation.isTeamComplete()).isFalse();
    }

    @Test
    void aTeamWithItsRequiredSupervisorIsComplete() {
        Reservation reservation = aReservation().withOffering(anOffering().sendingASupervisorFrom(2, 3)).reserved();

        reservation.nameTeam(TeamAssignment.helpers("aina", "ben").withSupervisors("siti"), ROSTER, friday("12:00"));

        assertThat(reservation.isTeamComplete()).isTrue();
    }

    @Test
    void aTeamNotYetNamedIsNotComplete() {
        assertThat(aReservation().reserved().isTeamComplete()).isFalse();
    }
}
