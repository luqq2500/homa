package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static reservation.domain.ReservationBuilders.ROSTER;
import static reservation.domain.ReservationBuilders.aBooking;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.anOffering;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.thursday;

import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import reservation.domain.exception.HelperNotOnRosterException;
import reservation.domain.exception.InvalidStatusTransitionException;
import reservation.domain.exception.MemberNamedTwiceException;
import reservation.domain.exception.ReservationDetailRequiredException;
import reservation.domain.exception.SupervisorNotOnRosterException;
import reservation.domain.exception.SupervisorRequiredException;
import reservation.domain.exception.TeamLockedException;
import reservation.domain.exception.TeamSizeMismatchException;

/**
 * Feature: Naming who is coming. Background: reserved for 2 helpers from Saturday 10:00 for 3 hours; the team is
 * Aina, Ben and Chan as helpers and Siti as a supervisor.
 */
class ReservationTeamNamingTest {

    @Test
    void theProviderNamesTheHelpers() {
        Reservation reservation = aReservation().reserved();

        reservation.nameTeam(TeamAssignment.helpers("aina", "ben"), ROSTER, friday("12:00"));

        assertThat(reservation.team()).contains(TeamAssignment.helpers("aina", "ben"));
    }

    @Test
    void theTeamMustMatchTheNumberOfHelpersBooked() {
        Reservation reservation = aReservation().reserved();

        assertThatThrownBy(() -> reservation.nameTeam(TeamAssignment.helpers("aina"), ROSTER, friday("12:00")))
                .isInstanceOf(TeamSizeMismatchException.class)
                .hasMessage("1 helpers named, but 2 were booked");
        assertThat(reservation.team()).isEmpty();
    }

    @Test
    void helpersMustBeOnTheTeamAsHelpers() {
        Reservation reservation = aReservation().reserved();

        assertThatThrownBy(() -> reservation.nameTeam(TeamAssignment.helpers("aina", "dina"), ROSTER, friday("12:00")))
                .isInstanceOf(HelperNotOnRosterException.class)
                .hasMessageContaining("dina");
        assertThatThrownBy(() -> reservation.nameTeam(TeamAssignment.helpers("aina", "siti"), ROSTER, friday("12:00")))
                .isInstanceOf(HelperNotOnRosterException.class)
                .hasMessageContaining("siti");
    }

    @Test
    void supervisorsMustBeOnTheTeamAsSupervisors() {
        Reservation reservation = aReservation().reserved();

        assertThatThrownBy(() -> reservation.nameTeam(
                        TeamAssignment.helpers("aina", "ben").withSupervisors("chan"), ROSTER, friday("12:00")))
                .isInstanceOf(SupervisorNotOnRosterException.class)
                .hasMessageContaining("chan");
        assertThat(reservation.team()).isEmpty();
    }

    @Test
    void theSamePersonCannotBeNamedTwice() {
        assertThatThrownBy(() -> TeamAssignment.helpers("aina", "siti").withSupervisors("siti"))
                .isInstanceOf(MemberNamedTwiceException.class)
                .hasMessageContaining("siti");
        assertThatThrownBy(() -> TeamAssignment.helpers(" ")).isInstanceOf(ReservationDetailRequiredException.class);
        assertThatThrownBy(() -> TeamAssignment.helpers("aina").withSupervisors(" "))
                .isInstanceOf(ReservationDetailRequiredException.class)
                .hasMessageContaining("supervisor");
    }

    @Test
    void theTeamIsKeptByRole() {
        TeamRoster roster = TeamRoster.ofHelpers("aina", "ben").withSupervisors("siti");

        assertThat(roster.helperIds()).containsExactlyInAnyOrder("aina", "ben");
        assertThat(roster.supervisorIds()).containsExactly("siti");
    }

    @Test
    void everyoneNamedIsTheHelpersAndTheSupervisors() {
        TeamAssignment team = TeamAssignment.helpers("aina", "ben").withSupervisors("siti");

        assertThat(team.members()).containsExactlyInAnyOrder("aina", "ben", "siti");
        assertThat(team.supervisors()).containsExactly("siti");
    }

    @Test
    void aProviderWhoIsAHelperOnTheirOwnTeamCanBeNamed() {
        Reservation reservation = aReservation().reserved();

        reservation.nameTeam(TeamAssignment.helpers("provider-1", "aina"), TeamRoster.ofHelpers("provider-1", "aina"), friday("12:00"));

        assertThat(reservation.team().orElseThrow().helpers()).contains("provider-1");
    }

    @Test
    void theTeamCanChangeUntilTheBookedStart() {
        Reservation reservation = aReservation().reservedWithTeamNamed();

        reservation.nameTeam(TeamAssignment.helpers("ben", "chan"), ROSTER, saturday("09:59"));

        assertThat(reservation.team()).contains(TeamAssignment.helpers("ben", "chan"));
    }

    @Test
    void theTeamCannotChangeFromTheBookedStart() {
        Reservation reservation = aReservation().reservedWithTeamNamed();

        assertThatThrownBy(() -> reservation.nameTeam(TeamAssignment.helpers("ben", "chan"), ROSTER, saturday("10:00")))
                .isInstanceOf(TeamLockedException.class);
        assertThat(reservation.team()).contains(TeamAssignment.helpers("aina", "ben"));
    }

    @Test
    void onlyAReservedBookingGetsATeam() {
        assertThatThrownBy(() -> aReservation().requested().nameTeam(TeamAssignment.helpers("aina", "ben"), ROSTER, thursday("10:00")))
                .isInstanceOf(InvalidStatusTransitionException.class);
    }

    /** Feature: Supervisors. The provider sends a supervisor from 3 helpers or from 4 hours. */
    @Nested
    class Supervisors {

        private Reservation reservedFor(int helpers, int hours) {
            return aReservation()
                    .withOffering(anOffering().sendingASupervisorFrom(3, 4))
                    .forBooking(aBooking().withHelpers(helpers).forHours(hours))
                    .reserved();
        }

        @Test
        void namingTheTeamWithoutARequiredSupervisorIsRefused() {
            Reservation reservation = reservedFor(3, 2);

            assertThatThrownBy(() -> reservation.nameTeam(TeamAssignment.helpers("aina", "ben", "chan"), ROSTER, friday("12:00")))
                    .isInstanceOf(SupervisorRequiredException.class);
            assertThat(reservation.team()).isEmpty();
        }

        @Test
        void theHoursThresholdAloneRequiresASupervisor() {
            Reservation reservation = reservedFor(1, 4);

            assertThatThrownBy(() -> reservation.nameTeam(TeamAssignment.helpers("aina"), ROSTER, friday("12:00")))
                    .isInstanceOf(SupervisorRequiredException.class);
        }

        @Test
        void aRequiredSupervisorCanBeNamed() {
            Reservation reservation = reservedFor(1, 4);

            reservation.nameTeam(TeamAssignment.helpers("aina").withSupervisors("siti"), ROSTER, friday("12:00"));

            assertThat(reservation.team().orElseThrow().supervisors()).containsExactly("siti");
        }

        @Test
        void aBookingBelowBothThresholdsNeedsNoSupervisor() {
            Reservation reservation = reservedFor(2, 3);

            reservation.nameTeam(TeamAssignment.helpers("aina", "ben"), ROSTER, friday("12:00"));

            assertThat(reservation.team().orElseThrow().supervisors()).isEmpty();
        }

        @Test
        void aProviderCanSendASupervisorWhenNoneIsRequired() {
            Reservation reservation = aReservation().reserved();

            reservation.nameTeam(TeamAssignment.helpers("aina", "ben").withSupervisors("siti"), ROSTER, friday("12:00"));

            assertThat(reservation.team().orElseThrow().members()).hasSize(3);
        }

        @Test
        void supervisorsAreNotCharged() {
            Reservation reservation = reservedFor(3, 2);
            reservation.nameTeam(TeamAssignment.helpers("aina", "ben", "chan").withSupervisors("siti"), ROSTER, friday("12:00"));

            assertThat(reservation.price().hoursFee()).isEqualTo(kernel.Money.parse("180.00"));
        }

        @Test
        void theRuleIsCopiedWithTheOffering() {
            SupervisorRule rule = reservedFor(1, 2).offering().supervisorRule().orElseThrow();

            assertThat(rule.requiresSupervisor(3, 1)).isTrue();
            assertThat(rule.requiresSupervisor(1, 4)).isTrue();
            assertThat(rule.requiresSupervisor(2, 3)).isFalse();
            assertThat(new SupervisorRule(java.util.OptionalInt.empty(), java.util.OptionalInt.of(4)).requiresSupervisor(10, 3)).isFalse();
            assertThatThrownBy(() -> new SupervisorRule(null, java.util.OptionalInt.empty())).isInstanceOf(NullPointerException.class);
            assertThatThrownBy(() -> new SupervisorRule(java.util.OptionalInt.empty(), null)).isInstanceOf(NullPointerException.class);
            assertThat(Set.of(rule)).hasSize(1);
        }
    }
}
