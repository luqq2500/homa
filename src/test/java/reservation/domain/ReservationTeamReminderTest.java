package reservation.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static reservation.domain.ReservationBuilders.ROSTER;
import static reservation.domain.ReservationBuilders.aReservation;
import static reservation.domain.ReservationBuilders.friday;
import static reservation.domain.ReservationBuilders.saturday;
import static reservation.domain.ReservationBuilders.thursday;

import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/** Feature: Reminding the provider to name the team. Background: reserved for Saturday 10:00, no team named. */
class ReservationTeamReminderTest {

    static Stream<Arguments> reminders() {
        return Stream.of(
                Arguments.of(friday("12:00"), false),
                Arguments.of(saturday("03:59"), false),
                Arguments.of(saturday("04:00"), true),
                Arguments.of(saturday("09:59"), true),
                Arguments.of(saturday("10:00"), false));
    }

    @ParameterizedTest(name = "the team reminder is due at {0}: {1}")
    @MethodSource("reminders")
    void theTeamReminderIsDueFromSixHoursBeforeTheStartUntilTheStart(Instant at, boolean due) {
        assertThat(aReservation().reserved().isTeamReminderDue(at)).isEqualTo(due);
    }

    @Test
    void noReminderOnceTheTeamIsNamed() {
        Reservation reservation = aReservation().reserved();
        reservation.nameTeam(TeamAssignment.helpers("aina", "ben"), ROSTER, friday("12:00"));

        assertThat(reservation.isTeamReminderDue(saturday("05:00"))).isFalse();
    }

    @Test
    void noReminderForARequestOrACancelledBooking() {
        assertThat(aReservation().requested().isTeamReminderDue(saturday("05:00"))).isFalse();

        Reservation cancelled = aReservation().reserved();
        cancelled.cancelByCustomer(thursday("12:00"));
        assertThat(cancelled.isTeamReminderDue(saturday("05:00"))).isFalse();
    }
}
