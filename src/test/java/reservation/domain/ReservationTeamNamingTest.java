package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.ROSTER;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.thursday;

import org.junit.jupiter.api.Test;
import reservation.domain.exception.HelperNotOnRosterException;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.LeadNotInTeamException;
import reservation.domain.exception.ReservationDetailRequiredException;
import reservation.domain.exception.TeamLockedException;
import reservation.domain.exception.TeamSizeMismatchException;

/** Feature: Naming who is coming. Background: reserved for 2 helpers from Saturday 10:00; the verified roster is Aina, Ben and Chan. */
class ReservationTeamNamingTest {

    @Test
    void theProviderNamesTheLeadAndHelpers() {
        Reservation reservation = aReservation().reserved();

        reservation.nameTeam(TeamAssignment.of("aina", "aina", "ben"), ROSTER, friday("12:00"));

        assertThat(reservation.team()).contains(TeamAssignment.of("aina", "aina", "ben"));
    }

    @Test
    void theTeamMustMatchTheNumberOfHelpersBooked() {
        Reservation reservation = aReservation().reserved();

        assertThatThrownBy(() -> reservation.nameTeam(TeamAssignment.of("aina", "aina"), ROSTER, friday("12:00")))
                .isInstanceOf(TeamSizeMismatchException.class)
                .hasMessage("1 helpers named, but 2 were booked");
        assertThat(reservation.team()).isEmpty();
    }

    @Test
    void helpersMustBeOnTheVerifiedRoster() {
        Reservation reservation = aReservation().reserved();

        assertThatThrownBy(() -> reservation.nameTeam(TeamAssignment.of("aina", "aina", "dina"), ROSTER, friday("12:00")))
                .isInstanceOf(HelperNotOnRosterException.class)
                .hasMessageContaining("dina");
    }

    @Test
    void theLeadMustBeOneOfTheNamedHelpers() {
        assertThatThrownBy(() -> TeamAssignment.of("chan", "aina", "ben")).isInstanceOf(LeadNotInTeamException.class);
        assertThatThrownBy(() -> TeamAssignment.of(" ", "aina", "ben")).isInstanceOf(ReservationDetailRequiredException.class);
    }

    @Test
    void aProviderWhoIsTheirOwnHelperCanLeadTheTeam() {
        Reservation reservation = aReservation().reserved();

        reservation.nameTeam(TeamAssignment.of("provider-1", "provider-1", "aina"), TeamRoster.of("provider-1", "aina"), friday("12:00"));

        assertThat(reservation.team().orElseThrow().lead()).isEqualTo("provider-1");
    }

    @Test
    void theTeamCanChangeUntilTheBookedStart() {
        Reservation reservation = aReservation().reservedWithTeamNamed();

        reservation.nameTeam(TeamAssignment.of("ben", "ben", "chan"), ROSTER, saturday("09:59"));

        assertThat(reservation.team()).contains(TeamAssignment.of("ben", "ben", "chan"));
    }

    @Test
    void theTeamCannotChangeFromTheBookedStart() {
        Reservation reservation = aReservation().reservedWithTeamNamed();

        assertThatThrownBy(() -> reservation.nameTeam(TeamAssignment.of("ben", "ben", "chan"), ROSTER, saturday("10:00")))
                .isInstanceOf(TeamLockedException.class);
        assertThat(reservation.team()).contains(TeamAssignment.of("aina", "aina", "ben"));
    }

    @Test
    void onlyAReservedBookingGetsATeam() {
        assertThatThrownBy(() -> aReservation().requested().nameTeam(TeamAssignment.of("aina", "aina", "ben"), ROSTER, thursday("10:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }
}
